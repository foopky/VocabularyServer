package vocabulary.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import vocabulary.app.dto.WordMeaningRequestDTO;
import vocabulary.app.entity.WordMeaningCache;
import vocabulary.app.repository.WordMeaningCacheRepository;
import vocabulary.app.security.UserRateLimiter;

import java.util.Locale;
import java.util.Optional;

// 확장의 "단어 추가" 모달에서 meaning 칸을 채우기 위한 뜻 조회.
//
// 호출량 자체는 작다. 자막 줄마다 도는 번역과 달리 사용자가 "+" 를 누를 때만 돈다.
// 대신 사용자 전체를 가로지르는 캐시의 이득이 크다 — 학습자들이 담는 단어는 겹치는 폭이 넓다.
@Slf4j
@Service
public class WordMeaningService {

    private final WordMeaningCacheRepository cacheRepository;
    private final OpenAIService openAIService;
    private final UserRateLimiter rateLimiter;
    private final int maxMeaningLength;

    public WordMeaningService(WordMeaningCacheRepository cacheRepository,
                              OpenAIService openAIService,
                              UserRateLimiter rateLimiter,
                              @Value("${wordmeaning.max-length:60}") int maxMeaningLength) {
        this.cacheRepository = cacheRepository;
        this.openAIService = openAIService;
        this.rateLimiter = rateLimiter;
        this.maxMeaningLength = maxMeaningLength;
    }

    // 뜻 한 줄을 돌려준다. 판정하지 못하면 null (호출자는 200 + {"meaning": null} 로 내려준다).
    //
    // 트랜잭션을 걸지 않는다. LLM 호출이 수 초 걸리는데 그 시간 내내 DB 커넥션을 붙들고 있을 이유가 없다.
    public String resolveMeaning(Long userId, WordMeaningRequestDTO request) {
        String baseForm = requireText(request.baseForm(), "base_form");
        String reading = requireText(request.reading(), "reading");
        String targetLanguage = normalizeLanguage(requireText(request.targetLanguage(), "target_language"));
        // word 는 참고용이라 없어도 조회는 된다. pos / pos_detail 도 선택 입력이다.
        String word = trimToNull(request.word());
        String pos = trimToEmpty(request.pos());
        String posDetail = trimToNull(request.posDetail());

        rateLimiter.check(userId);

        Optional<WordMeaningCache> cached =
                cacheRepository.findByBaseFormAndReadingAndPosAndTargetLanguage(baseForm, reading, pos, targetLanguage);
        if (cached.isPresent()) {
            return cached.get().getMeaning();
        }

        String raw = openAIService.generateWordMeaning(baseForm, word, reading, pos, posDetail, targetLanguage);
        String meaning = sanitize(raw);
        if (meaning == null) {
            // 못 찾은 결과는 캐시하지 않는다. 한 번의 실패를 영구히 못박는 것보다,
            // 나중에 다시 물어봐서 풀리는 편이 낫다. 호출량이 작아서 재조회 비용도 작다.
            log.info("뜻을 판정하지 못했습니다: baseForm={}, reading={}, target={}", baseForm, reading, targetLanguage);
            return null;
        }

        cache(baseForm, reading, pos, targetLanguage, meaning);
        return meaning;
    }

    private void cache(String baseForm, String reading, String pos, String targetLanguage, String meaning) {
        try {
            cacheRepository.save(WordMeaningCache.of(baseForm, reading, pos, targetLanguage, meaning));
        } catch (DataIntegrityViolationException e) {
            // 같은 단어를 두 사용자가 동시에 조회하면 둘 다 캐시 미스가 나서 둘 다 insert 를 시도한다.
            // 유니크 제약에 걸린 쪽은 그냥 넘어가면 된다 — 이미 같은 뜻이 저장돼 있고, 응답에는 영향이 없다.
            // (이 메서드가 트랜잭션 밖이라 save 는 자기 트랜잭션에서만 롤백된다.)
            log.debug("캐시 중복 저장: baseForm={}, reading={}", baseForm, reading);
        } catch (Exception e) {
            // 캐시는 부가 기능이다. 저장에 실패해도 이미 구한 뜻은 그대로 내려준다.
            log.warn("뜻 캐시 저장 실패: baseForm={}, reading={}", baseForm, reading, e);
        }
    }

    // 모델 출력을 모달의 3줄짜리 textarea 에 들어갈 한 줄로 다듬는다.
    // 프롬프트로 길이를 제한하고 있지만, 지키지 않은 출력이 그대로 나가면 칸이 못 쓰게 되므로 여기서 한 번 더 자른다.
    private String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String meaning = raw.strip();
        if (meaning.isEmpty()) {
            return null;
        }

        // 설명이 딸려 오면 첫 줄만 쓴다.
        int newline = meaning.indexOf('\n');
        if (newline >= 0) {
            meaning = meaning.substring(0, newline).strip();
        }

        // 따옴표로 감싸 오거나, 번호/불릿을 붙여 오는 경우.
        meaning = meaning.replaceAll("^[\"'“”‘’\\[(]+", "")
                .replaceAll("[\"'“”‘’\\])]+$", "")
                .replaceAll("^\\s*(?:[-*•]|\\d+[.)])\\s*", "")
                .strip();
        // 문장으로 끝나면 마침표를 떼어 뜻만 남긴다.
        meaning = meaning.replaceAll("[.。]+$", "").strip();
        // 줄바꿈 대신 들어온 여러 칸의 공백 정리.
        meaning = meaning.replaceAll("\\s+", " ");

        if (meaning.isEmpty()) {
            return null;
        }
        return trimToLimit(meaning);
    }

    // 길이를 넘으면 쉼표 경계에서 자른다. 뜻 하나조차 넘치면 그때만 글자 단위로 자른다.
    private String trimToLimit(String meaning) {
        if (meaning.length() <= maxMeaningLength) {
            return meaning;
        }

        StringBuilder trimmed = new StringBuilder();
        for (String sense : meaning.split("[,、，]")) {
            String candidate = sense.strip();
            if (candidate.isEmpty()) {
                continue;
            }
            int lengthIfAdded = trimmed.isEmpty() ? candidate.length() : trimmed.length() + 2 + candidate.length();
            if (lengthIfAdded > maxMeaningLength) {
                break;
            }
            if (!trimmed.isEmpty()) {
                trimmed.append(", ");
            }
            trimmed.append(candidate);
        }

        if (trimmed.isEmpty()) {
            return meaning.substring(0, maxMeaningLength).strip();
        }
        return trimmed.toString();
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " 은(는) 필수입니다.");
        }
        return trimmed;
    }

    // 캐시 키가 "ko" 와 "KO" 로 갈리지 않도록 맞춰 둔다.
    private String normalizeLanguage(String targetLanguage) {
        return targetLanguage.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // pos 는 캐시 키의 일부인데 선택 입력이다.
    // PostgreSQL 의 UNIQUE 는 NULL 끼리를 서로 다른 값으로 보므로, 빈 문자열로 맞춰야 중복 행이 쌓이지 않는다.
    private String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }
}
