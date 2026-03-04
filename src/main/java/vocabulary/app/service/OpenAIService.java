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

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class OpenAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Duration timeout;

    public OpenAIService(@Value("${openai.api.key}") String apiKey,
                         @Value("${openai.model:gpt-5-nano-2025-08-07}") String model) {
        this.model = model;
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
            String prompt = buildPrompt(word, language, style);

            Map<String, Object> messageSystem = Map.of(
                    "role", "system",
                    "content", "You are a helpful assistant that only outputs a JSON array of 3 objects."
            );
            Map<String, Object> messageUser = Map.of(
                    "role", "user",
                    "content", prompt
            );

            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", List.of(messageSystem, messageUser));
            request.put("temperature", 1);
            request.put("max_completion_tokens", 5000);

            Mono<String> respMono = webClient.post()
                    .uri("/chat/completions")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout);

            String respBody = respMono.block();
            if (respBody == null) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(respBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return Collections.emptyList();
            }

            String content = contentNode.asText().trim();

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
}