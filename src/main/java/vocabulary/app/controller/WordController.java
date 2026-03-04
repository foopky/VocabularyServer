package vocabulary.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordInFolderId;
import vocabulary.app.service.WordService;

import java.util.List;

@RestController
@RequestMapping("/words")
public class WordController {
    private final WordService wordService;

    @Autowired
    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping("/{language}")
    public ResponseEntity<List<Word>> getAll(@PathVariable("language") String language) {
        return ResponseEntity.ok(wordService.getAll(language));
    }

    @GetMapping("/{language}/learned")
    public ResponseEntity<List<Word>>getByLearned(@PathVariable("language") String language,
                                   @RequestParam("learned") boolean learned) {
        return ResponseEntity.ok(wordService.getByLearned(language, learned));
    }

    @Operation(summary = "Word 생성")
    @PostMapping
    public ResponseEntity<Word> save(@RequestBody Word word) {
        return ResponseEntity.ok(wordService.save(word.getLanguage(),word));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody WordInFolderId wordInFolderId) {
        wordService.delete(wordInFolderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Word> update(@RequestBody Word word) {
        return ResponseEntity.ok(wordService.save(word.getLanguage(),word));
    }
}
