package vocabulary.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vocabulary.app.dto.WordMeaningRequestDTO;
import vocabulary.app.entity.WordMeaningCache;
import vocabulary.app.exception.LlmUnavailableException;
import vocabulary.app.exception.RateLimitExceededException;
import vocabulary.app.repository.WordMeaningCacheRepository;
import vocabulary.app.security.UserRateLimiter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WordMeaningServiceTest {

    private static final long USER_ID = 1L;

    private WordMeaningCacheRepository cacheRepository;
    private OpenAIService openAIService;
    private WordMeaningService service;

    @BeforeEach
    void setUp() {
        cacheRepository = mock(WordMeaningCacheRepository.class);
        openAIService = mock(OpenAIService.class);
        // 테스트에서 호출 제한에 걸리지 않도록 넉넉하게
        service = new WordMeaningService(cacheRepository, openAIService, new UserRateLimiter(1000, 60), 60);
        when(cacheRepository.findByBaseFormAndReadingAndPosAndTargetLanguage(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private WordMeaningRequestDTO request(String baseForm, String word, String reading,
                                          String pos, String posDetail, String targetLanguage) {
        return new WordMeaningRequestDTO(baseForm, word, reading, pos, posDetail, targetLanguage);
    }

    // 확인의 기준점: 표기가 같고 읽기만 다른 요청이 서로 다른 캐시 항목이 되어야 한다.
    // 여기가 갈리지 않으면 生物(ナマモノ)가 "생물"로 조용히 틀린다.
    @Test
    @DisplayName("읽기가 다르면 캐시가 갈리고, 읽기가 그대로 LLM 에 전달된다")
    void readingSeparatesHomographs() {
        when(openAIService.generateWordMeaning(eq("生物"), any(), eq("ナマモノ"), any(), any(), any()))
                .thenReturn("날것, 신선식품");
        when(openAIService.generateWordMeaning(eq("生物"), any(), eq("セイブツ"), any(), any(), any()))
                .thenReturn("생물, 살아있는 것");

        String namamono = service.resolveMeaning(USER_ID,
                request("生物", "生物", "ナマモノ", "名詞", "一般", "ko"));
        String seibutsu = service.resolveMeaning(USER_ID,
                request("生物", "生物", "セイブツ", "名詞", "一般", "ko"));

        assertThat(namamono).isEqualTo("날것, 신선식품");
        assertThat(seibutsu).isEqualTo("생물, 살아있는 것");

        // 캐시 조회도 읽기별로 따로 나가야 한다
        verify(cacheRepository).findByBaseFormAndReadingAndPosAndTargetLanguage("生物", "ナマモノ", "名詞", "ko");
        verify(cacheRepository).findByBaseFormAndReadingAndPosAndTargetLanguage("生物", "セイブツ", "名詞", "ko");
    }

    @Test
    @DisplayName("캐시에 있으면 LLM 을 호출하지 않는다")
    void cacheHitSkipsLlm() {
        when(cacheRepository.findByBaseFormAndReadingAndPosAndTargetLanguage("食べる", "タベル", "動詞", "ko"))
                .thenReturn(Optional.of(WordMeaningCache.of("食べる", "タベル", "動詞", "ko", "먹다")));

        String meaning = service.resolveMeaning(USER_ID,
                request("食べる", "食べ", "タベル", "動詞", "自立", "ko"));

        assertThat(meaning).isEqualTo("먹다");
        verifyNoInteractions(openAIService);
    }

    @Test
    @DisplayName("조회는 활용형이 아니라 사전형으로 한다")
    void looksUpByBaseForm() {
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any())).thenReturn("걸다, 매달다");

        service.resolveMeaning(USER_ID, request("掛ける", "掛け", "カケ", "動詞", "自立", "ko"));

        verify(cacheRepository).findByBaseFormAndReadingAndPosAndTargetLanguage("掛ける", "カケ", "動詞", "ko");
        verify(openAIService).generateWordMeaning("掛ける", "掛け", "カケ", "動詞", "自立", "ko");
    }

    @Test
    @DisplayName("뜻을 판정하지 못하면 null 을 돌려주고 캐시하지 않는다")
    void unresolvedMeaningIsNotCached() {
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any())).thenReturn(null);

        String meaning = service.resolveMeaning(USER_ID, request("なんとか", "なんとか", "ナントカ", null, null, "ko"));

        assertThat(meaning).isNull();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("LLM 호출 실패는 삼키지 않고 그대로 올린다 (200 + null 로 위장하면 안 된다)")
    void llmFailurePropagates() {
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any()))
                .thenThrow(new LlmUnavailableException("boom"));

        assertThatThrownBy(() -> service.resolveMeaning(USER_ID, request("食べる", "食べ", "タベル", "動詞", null, "ko")))
                .isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    @DisplayName("pos 가 없으면 빈 문자열로 정규화해 캐시 키를 만든다")
    void missingPosIsNormalized() {
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any())).thenReturn("먹다");

        service.resolveMeaning(USER_ID, request("食べる", "食べる", "タベル", "  ", null, "KO"));

        // pos 는 "" 로, 언어 코드는 소문자로 맞춰 캐시가 갈리지 않게 한다
        verify(cacheRepository).findByBaseFormAndReadingAndPosAndTargetLanguage("食べる", "タベル", "", "ko");
    }

    @Test
    @DisplayName("동시 조회로 캐시 저장이 중복되어도 응답은 정상이다")
    void duplicateCacheWriteIsIgnored() {
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any())).thenReturn("먹다");
        when(cacheRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        String meaning = service.resolveMeaning(USER_ID, request("食べる", "食べる", "タベル", "動詞", null, "ko"));

        assertThat(meaning).isEqualTo("먹다");
    }

    @Test
    @DisplayName("필수 필드가 빠지면 400 (IllegalArgumentException)")
    void missingRequiredFieldsRejected() {
        assertThatThrownBy(() -> service.resolveMeaning(USER_ID, request(null, "生物", "セイブツ", "名詞", null, "ko")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base_form");

        assertThatThrownBy(() -> service.resolveMeaning(USER_ID, request("生物", "生物", "  ", "名詞", null, "ko")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reading");

        assertThatThrownBy(() -> service.resolveMeaning(USER_ID, request("生物", "生物", "セイブツ", "名詞", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target_language");

        verifyNoInteractions(openAIService);
    }

    // 모달의 3줄짜리 textarea 에 문단이 들어오면 칸을 못 쓴다.
    @Test
    @DisplayName("따옴표·번호·마침표·여러 줄은 한 줄 뜻으로 다듬는다")
    void sanitizesModelOutput() {
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any()))
                .thenReturn("  \"걸다, 매달다.\"\n(예: 服を掛ける - 옷을 걸다)  ");

        String meaning = service.resolveMeaning(USER_ID, request("掛ける", "掛け", "カケ", "動詞", null, "ko"));

        assertThat(meaning).isEqualTo("걸다, 매달다");
    }

    @Test
    @DisplayName("길이를 넘기면 쉼표 경계에서 자른다")
    void trimsAtSenseBoundary() {
        WordMeaningService shortLimit =
                new WordMeaningService(cacheRepository, openAIService, new UserRateLimiter(1000, 60), 10);
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any()))
                .thenReturn("걸다, 매달다, 씌우다, 소요되다");

        String meaning = shortLimit.resolveMeaning(USER_ID, request("掛ける", "掛け", "カケ", "動詞", null, "ko"));

        assertThat(meaning).isEqualTo("걸다, 매달다");
        assertThat(meaning.length()).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("호출 제한을 넘기면 LLM 을 부르지 않고 429 로 끊는다")
    void rateLimitStopsLlmCall() {
        WordMeaningService limited =
                new WordMeaningService(cacheRepository, openAIService, new UserRateLimiter(2, 60), 60);
        when(openAIService.generateWordMeaning(any(), any(), any(), any(), any(), any())).thenReturn("먹다");

        WordMeaningRequestDTO req = request("食べる", "食べる", "タベル", "動詞", null, "ko");
        limited.resolveMeaning(USER_ID, req);
        limited.resolveMeaning(USER_ID, req);

        assertThatThrownBy(() -> limited.resolveMeaning(USER_ID, req))
                .isInstanceOf(RateLimitExceededException.class);
        // 제한은 사용자별이므로 다른 사용자는 영향을 받지 않는다
        assertThat(limited.resolveMeaning(2L, req)).isEqualTo("먹다");
        verify(openAIService, times(3)).generateWordMeaning(any(), any(), any(), any(), any(), any());
    }
}
