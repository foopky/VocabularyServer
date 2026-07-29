package vocabulary.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.PageResponseDTO;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.security.OwnershipGuard;
import vocabulary.app.service.WordFolderService;

import java.util.List;

@RestController
@RequestMapping("/folder")
public class WordFolderController {
    private final WordFolderService wordFolderService;
    private final OwnershipGuard ownershipGuard;

    public WordFolderController(WordFolderService wordFolderService, OwnershipGuard ownershipGuard){
        this.wordFolderService = wordFolderService;
        this.ownershipGuard = ownershipGuard;
    }


    @Operation(summary = "모든 폴더 탐색")
    @GetMapping("/getfolderLanguage/{language}")
    public List<WordFolder> getAllWordFolder(@PathVariable("language") String language){
        return wordFolderService.getAllWordFolder(language);
    }

    @Operation(summary = "유저에 관한 폴더 탐색")
    @GetMapping("/getfolderUser/{userId}")
    public List<WordFolder> getWordFolderOnUser(@PathVariable("userId") Long userId,
                                                Authentication authentication){
        ownershipGuard.checkUser(userId, authentication);
        return wordFolderService.getWordFolderOnUser(userId);
    }

    @Operation(summary = "폴더의 단어 탐색 (페이징)",
            description = "page(0부터), size, sort 파라미터를 지원한다. 예) ?page=0&size=20&sort=word,asc")
    @GetMapping("/getwords/{folderId}")
    public PageResponseDTO<Word> getALlWordsInFolder(
            @PathVariable("folderId") Long folderId,
            // 정렬을 지정하지 않으면 페이지마다 순서가 달라져 단어가 중복/누락될 수 있으므로 id 기준 기본 정렬을 둔다.
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable,
            Authentication authentication
    ){
        // 공유 게시판에 올라온 폴더는 남의 것이어도 열람할 수 있어야 하므로 소유자 검사가 아닌 열람 검사를 쓴다.
        ownershipGuard.checkFolderReadable(folderId, authentication);
        return PageResponseDTO.from(wordFolderService.getAllWordsInFolder(folderId, pageable));
    }

    @Operation(summary = "WordFolder 생성, User 할당")
    @PostMapping("/wordfolder")
    public Object saveWordFolder(@RequestBody WordFolder wordFolder,
                                 @RequestParam(name = "user_id")Long userId,
                                 Authentication authentication){
        ownershipGuard.checkUser(userId, authentication);
        return wordFolderService.saveWordFolder(wordFolder.getLanguage(),wordFolder,userId);
    }

    @Operation(summary="Folder에 Word 추가")
    @PostMapping("/addwordtofolder/{language}/{wordId}/{folderId}")
    public void addWordToFolder(@PathVariable("language") String language , @PathVariable("wordId") Long wordId, @PathVariable("folderId") Long folderId,
                                Authentication authentication){
        ownershipGuard.checkFolderOwner(folderId, authentication);
        wordFolderService.addWordToFolder(language, wordId, folderId);
    }

    @Operation(summary = "WordFolder 폼으로 저장")
    @PostMapping("/SharedFolderToMyWordFolder")
    public void sharedFolderToMyWordFolder(@RequestParam("userId") Long userId, @RequestBody WordFolder wordFolder,
                                           Authentication authentication){
        ownershipGuard.checkUser(userId, authentication);
        wordFolderService.sharedFolderToMyWordFolder(userId, wordFolder);
    }

    @Operation(summary = "Folder 삭제")
    @DeleteMapping("/{folderId}")
    public void deleteFolder(@PathVariable("folderId") Long folderId,
                             Authentication authentication){
        ownershipGuard.checkFolderOwner(folderId, authentication);
        wordFolderService.deleteFolder(folderId);
    }
}
