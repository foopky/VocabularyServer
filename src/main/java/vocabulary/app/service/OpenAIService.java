package vocabulary.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vocabulary.app.exception.LlmUnavailableException;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class OpenAIService {

    // 뜻을 판정하지 못했을 때 모델이 내보내는 표식. 이 값이 오면 "뜻 없음"(null)으로 바꾼다.
    private static final String UNKNOWN_MARKER = "UNKNOWN";

    // 언어 코드를 프롬프트에 쓸 언어 이름으로. 모르는 코드는 코드 그대로 넣는다
    // (모델이 대개 알아듣고, 못 알아들으면 UNKNOWN 이 와서 200 + null 로 흘러간다).
    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("ko", "Korean"),
            Map.entry("en", "English"),
            Map.entry("ja", "Japanese"),
            Map.entry("zh", "Chinese"),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("de", "German"),
            Map.entry("pt", "Portuguese"),
            Map.entry("ru", "Russian"),
            Map.entry("vi", "Vietnamese"),
            Map.entry("th", "Thai"),
            Map.entry("id", "Indonesian")
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Duration timeout;
    private final String meaningReasoningEffort;
    private final int meaningMaxCompletionTokens;

    public OpenAIService(@Value("${openai.api.key}") String apiKey,
                         @Value("${openai.model:gpt-5-nano-2025-08-07}") String model,
                         @Value("${openai.meaning.reasoning-effort:low}") String meaningReasoningEffort,
                         @Value("${openai.meaning.max-completion-tokens:3000}") int meaningMaxCompletionTokens) {
        this.model = model;
        this.meaningReasoningEffort = meaningReasoningEffort;
        this.meaningMaxCompletionTokens = meaningMaxCompletionTokens;
        this.objectMapper = new ObjectMapper();
        this.timeout = Duration.ofSeconds(60);
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public record ExampleResult(String sentence, String meaning) {}

    public List<ExampleResult> generateExamples(String word, String language, String style) {
        try {
            String content = chatCompletion(
                    "You are a helpful assistant that only outputs a JSON array of 3 objects.",
                    buildPrompt(word, language, style),
                    5000,
                    null
            );
            if (content == null) {
                return Collections.emptyList();
            }

            try {
                return objectMapper.readValue(content, new TypeReference<List<ExampleResult>>() {});
            } catch (Exception e) {
                String[] lines = content.split("\\r?\\n");
                List<ExampleResult> results = new ArrayList<>();
                for (String line : lines) {
                    String cleaned = line.trim()
                            .replaceAll("^\\d+\\.|^[\"']|[\"']$|^[-]\\s*", "")
                            .trim();
                    if (!cleaned.isEmpty()) {
                        results.add(new ExampleResult(cleaned, String.format("(%s) %s", language, cleaned)));
                    }
                }
                return results.isEmpty() ? Collections.emptyList() : results.subList(0, Math.min(3, results.size()));
            }
        } catch (Exception ex) {
            log.error("Error generating examples from OpenAI: ", ex);
            return Collections.emptyList();
        }
    }

    // 일본어 단어 하나의 뜻을 한 줄로 받아온다.
    //
    // 반환 null = 모델이 이 단어를 판정하지 못함 (호출은 정상적으로 끝났다).
    // 호출 자체가 실패하면 LlmUnavailableException 을 던진다 — 위쪽에서 503 으로 나가야 하므로
    // generateExamples 처럼 예외를 삼켜 "결과 없음"으로 위장하면 안 된다.
    public String generateWordMeaning(String baseForm, String word, String reading,
                                      String pos, String posDetail, String targetLanguage) {
        String languageName = languageName(targetLanguage);

        // reasoning effort 와 토큰 예산은 application.properties 의 설명을 참고할 것.
        // 너무 높으면 추론 토큰만 쓰다 본문이 비고, 너무 낮으면 읽기를 무시하고 흔한 뜻으로 끌려간다.
        String content = chatCompletion(
                "You are a Japanese-" + languageName + " dictionary. "
                        + "You output only the gloss for the entry you are given: "
                        + "no explanations, no example sentences, no romanization, no quotes.",
                buildMeaningPrompt(baseForm, word, reading, pos, posDetail, languageName),
                meaningMaxCompletionTokens,
                meaningReasoningEffort
        );

        if (content == null || content.isBlank()) {
            return null;
        }
        if (content.strip().equalsIgnoreCase(UNKNOWN_MARKER)) {
            return null;
        }
        return content;
    }

    // /chat/completions 를 한 번 호출하고 assistant 메시지 본문을 돌려준다.
    // 호출 실패는 LlmUnavailableException 으로 올린다.
    private String chatCompletion(String systemMessage, String userMessage,
                                  int maxCompletionTokens, String reasoningEffort) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        request.put("temperature", 1);
        request.put("max_completion_tokens", maxCompletionTokens);
        // 추론 모델이 아닌 모델로 바꿔 끼울 수 있도록, 비워두면 파라미터 자체를 보내지 않는다.
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            request.put("reasoning_effort", reasoningEffort.trim());
        }

        String respBody;
        try {
            Mono<String> respMono = webClient.post()
                    .uri("/chat/completions")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout);
            respBody = respMono.block();
        } catch (Exception e) {
            // 네트워크 오류 / 4xx·5xx 응답 / 타임아웃. 프롬프트는 남기지 않는다 (API 키가 헤더에 있고 로그가 길어진다).
            throw new LlmUnavailableException("OpenAI 호출에 실패했습니다.", e);
        }

        if (respBody == null) {
            throw new LlmUnavailableException("OpenAI 응답이 비어 있습니다.");
        }

        String content;
        String finishReason;
        try {
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode choice = root.path("choices").path(0);
            JsonNode contentNode = choice.path("message").path("content");
            content = (contentNode.isMissingNode() || contentNode.isNull()) ? null : contentNode.asText().trim();
            finishReason = choice.path("finish_reason").asText("");
        } catch (Exception e) {
            throw new LlmUnavailableException("OpenAI 응답을 해석하지 못했습니다.", e);
        }

        // 토큰 예산이 답을 내기도 전에 바닥났다. 모델이 "모르겠다"고 한 게 아니라 우리 쪽 설정 문제이므로
        // 빈 문자열을 "뜻 없음"으로 흘려보내면 안 된다. (추론 모델이 추론 토큰만 쓰고 끝나면 여기로 온다)
        if ("length".equals(finishReason) && (content == null || content.isBlank())) {
            throw new LlmUnavailableException(
                    "OpenAI 응답이 max_completion_tokens(" + maxCompletionTokens + ")에서 잘렸습니다.");
        }

        return content;
    }

    private String buildPrompt(String word, String language, String style) {
        return String.format(
                "Create exactly 3 example sentences that use the word \"%s\". " +
                "Write every sentence based on the word with a %s conversational tone. " +
                "For each sentence, return a JSON object with: " +
                "\"sentence\" and \"meaning\" (a concise translation of that sentence in %s). " +
                "Output only a pure JSON array of these three objects.",
                word, style, language
        );
    }

    // 프롬프트에서 제일 중요한 건 reading 이다.
    // 표기가 같아도 읽기가 다르면 다른 단어인 경우가 흔해서(生物 セイブツ/ナマモノ, 一日 イチニチ/ツイタチ),
    // 읽기를 그냥 나열만 하면 모델이 더 흔한 쪽 뜻으로 끌려간다. 그래서 명시적으로 못을 박는다.
    //
    // 테스트에서 프롬프트 내용을 직접 확인하려고 package-private 으로 둔다.
    String buildMeaningPrompt(String baseForm, String word, String reading,
                              String pos, String posDetail, String languageName) {
        boolean inflected = word != null && !word.isBlank() && !word.equals(baseForm);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Give the ").append(languageName)
                .append(" meaning of this Japanese dictionary entry.\n\n");

        prompt.append("Dictionary form: ").append(baseForm).append('\n');
        if (inflected) {
            // 자막에 뜬 활용형과 그 읽기. 사전형의 읽기가 아니므로 그렇게 밝혀서 준다.
            prompt.append("Form seen in the subtitle: ").append(word).append('\n');
            prompt.append("Reading of that form (katakana): ").append(reading).append('\n');
        } else {
            prompt.append("Reading (katakana): ").append(reading).append('\n');
        }
        if (pos != null && !pos.isBlank()) {
            prompt.append("Part of speech: ").append(pos);
            if (posDetail != null && !posDetail.isBlank()) {
                prompt.append(" / ").append(posDetail);
            }
            prompt.append('\n');
        }

        prompt.append("\nTranslate the dictionary form.\n");

        prompt.append("\nThe reading tells you which word this is. Japanese homographs are read differently ")
                .append("and mean different things — 生物 read セイブツ is \"living things\", but 生物 read ナマモノ ")
                .append("is \"raw food\"; 一日 read イチニチ is \"one day\", but read ツイタチ it is \"the 1st of the month\". ")
                .append("Translate the word that matches the reading above, even when the other reading is more common.\n");

        if (inflected) {
            // 이 안내가 없으면 모델이 "掛ける 는 カケル 인데 カケ 라니 모순"이라며 UNKNOWN 을 내놓는다.
            // 확장은 형태소 분석기가 뽑은 활용형의 읽기를 그대로 보내므로 정상적인 입력이다.
            prompt.append("\nThe reading above belongs to the inflected form, not to the dictionary form, ")
                    .append("so it is normally shorter than or different from the dictionary form's own reading ")
                    .append("(掛け is read カケ while its dictionary form 掛ける is read カケル). ")
                    .append("This is expected input, not a mismatch — never answer ").append(UNKNOWN_MARKER)
                    .append(" because of it.\n");
        }

        prompt.append("\nRules:\n")
                .append("- Output 1 to 3 senses separated by commas, most common sense first.\n")
                .append("- Keep the whole line short: about 20 characters for Korean, Japanese or Chinese, ")
                .append("about 40 for languages written in the Latin alphabet.\n")
                .append("- Match the part of speech: a verb entry gets a verb (\"걸다\"), not a noun phrase (\"거는 것\").\n")
                .append("- No sentence context is given, so use the most general everyday sense.\n")
                .append("- Output the gloss only: no quotes, notes, romanization, part-of-speech labels, ")
                .append("numbering, or the Japanese word itself.\n")
                .append("- Answer exactly ").append(UNKNOWN_MARKER)
                .append(" only if the dictionary form is not a Japanese word you can identify at all.");

        return prompt.toString();
    }

    private String languageName(String targetLanguage) {
        if (targetLanguage == null || targetLanguage.isBlank()) {
            return "Korean";
        }
        // "ko-KR" 같은 형태도 받아들인다.
        String code = targetLanguage.trim().toLowerCase(Locale.ROOT).split("[-_]")[0];
        return LANGUAGE_NAMES.getOrDefault(code, targetLanguage.trim());
    }
}
