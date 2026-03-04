package vocabulary.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vocabulary.app.service.OpenAIService;

import java.util.List;

@RestController
@RequestMapping("/")
public class WebController {
    private final OpenAIService openAIService;

    public WebController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }
    @GetMapping("/create_sentences")
    public ResponseEntity<List<OpenAIService.ExampleResult>> createSentences(
            @RequestParam("word") String word,
            @RequestParam("language") String language,
            @RequestParam("style") String style) {
        List<OpenAIService.ExampleResult> sentences = openAIService.generateExamples(word, language, style);
        return ResponseEntity.ok(sentences);
    }
}
