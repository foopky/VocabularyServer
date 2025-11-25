package vocabulary.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<Word> getAll(@PathVariable("language") String language) {
        return wordService.getAll(language);
    }

    @GetMapping("/{language}/learned")
    public List<Word>getByLearned(@PathVariable("language") String language,
                                   @RequestParam("learned") boolean learned) {
        return wordService.getByLearned(language, learned);
    }

    @Operation(summary = "Word 생성")
    @PostMapping
    public Object save(@RequestBody Word word) {
        return wordService.save(word.getLanguage(),word);
    }

    @DeleteMapping
    public void delete(@RequestBody WordInFolderId wordInFolderId) {
        wordService.delete(wordInFolderId);
    }


}
