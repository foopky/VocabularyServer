package vocabulary.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordInFolderId;
import vocabulary.app.security.OwnershipGuard;
import vocabulary.app.service.WordService;

import java.util.List;

@RestController
@RequestMapping("/words")
public class WordController {
    private final WordService wordService;
    private final OwnershipGuard ownershipGuard;

    @Autowired
    public WordController(WordService wordService, OwnershipGuard ownershipGuard) {
        this.wordService = wordService;
        this.ownershipGuard = ownershipGuard;
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

    // 폴더에서 단어를 빼는 API. 대상 폴더가 본인 것인지 확인해야 남의 폴더를 비울 수 없다.
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody WordInFolderId wordInFolderId,
                                    Authentication authentication) {
        ownershipGuard.checkFolderOwner(wordInFolderId.getFolderId(), authentication);
        wordService.delete(wordInFolderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Word> update(@RequestBody Word word) {
        return ResponseEntity.ok(wordService.save(word.getLanguage(),word));
    }
}
