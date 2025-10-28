package vocabulary.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.service.WordFolderService;

import java.util.List;

@RestController
@RequestMapping("/folder")
public class WordFolderController {
    private final WordFolderService wordFolderService;

    public WordFolderController(WordFolderService wordFolderService){
        this.wordFolderService = wordFolderService;
    }


    @Operation(summary = "모든 폴더 탐색")
    @GetMapping("/getfolder/{language}")
    public List<WordFolder> getAllWordFolder(@PathVariable("language") String language){
        return wordFolderService.getAllWordFolder(language);
    }

    @Operation(summary = "폴더의 단어 모두 탐색")
    @GetMapping("/getwords/{folderId}")
    public List<Word> getALlWordsInFolder(@PathVariable("folderId") Long folderId){
        return wordFolderService.getAllWordsInFolder(folderId);
    }

    @Operation(summary = "WordFolder 생성, User 할당")
    @PostMapping("/wordfolder")
    public Object saveWordFolder(@RequestBody WordFolder wordFolder, @RequestParam(name = "user_id")Long userId){
        return wordFolderService.saveWordFolder(wordFolder.getLanguage(),wordFolder,userId);
    }

    @Operation(summary="Folder에 Word 추가")
    @PostMapping("/addwordtofolder/{language}/{wordId}/{folderId}")
    public void addWordToFolder(@PathVariable("language") String language , @PathVariable("wordId") Long wordId, @PathVariable("folderId") Long folderId){
        wordFolderService.addWordToFolder(language, wordId, folderId);
    }

    @Operation(summary = "Folder 삭제")
    @DeleteMapping("/{folderId}")
    public void deleteFolder(@PathVariable("folderId") Long folderId){
        wordFolderService.deleteFolder(folderId);
    }
}
