package vocabulary.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.security.OwnershipGuard;
import vocabulary.app.service.SharedFolderService;

import java.util.List;
import java.util.NoSuchElementException;

// 조회(GET)는 공유 게시판이므로 로그인한 사용자에게 열어둔다.
// 등록/삭제만 본인 것으로 제한한다.
@RestController
@RequestMapping("/sharedFolder")
public class SharedFolderController {

    private final SharedFolderService sharedFolderService;
    private final OwnershipGuard ownershipGuard;

    public SharedFolderController(SharedFolderService sharedFolderService, OwnershipGuard ownershipGuard) {
        this.sharedFolderService = sharedFolderService;
        this.ownershipGuard = ownershipGuard;
    }

    @PostMapping
    public ResponseEntity<SharedFolder> addSharedFolder(
            @RequestBody SharedFolderDTO sharedFolderDTO,
            Authentication authentication) {
        // 본인 명의로만 공유할 수 있고, 공유 대상 폴더도 본인 것이어야 한다.
        ownershipGuard.checkUser(sharedFolderDTO.getUserId(), authentication);
        ownershipGuard.checkFolderOwner(sharedFolderDTO.getFolderId(), authentication);

        SharedFolder newSharedFolder = sharedFolderService.addSharedFolder(sharedFolderDTO);

        // HTTP 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(newSharedFolder);
    }

    @GetMapping
    public ResponseEntity<List<SharedFolder>> getSharedFolders() {
        List<SharedFolder> sharedFolders = sharedFolderService.getSharedFolders();
        return ResponseEntity.ok(sharedFolders);
    }

    @GetMapping("/{userId}/{folderId}")
    public ResponseEntity<SharedFolder> getDetailedSharedFolder(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "folderId") Long folderId) {
        try {
            SharedFolder detailedFolder = sharedFolderService.getDetailed(userId, folderId);
            return ResponseEntity.ok(detailedFolder);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SharedFolder>> getSharedFoldersByUser(
            @PathVariable(name = "userId") Long userId) {
        try {
            List<SharedFolder> sharedFolders = sharedFolderService.getSharedFoldersByUser(userId);
            return ResponseEntity.ok(sharedFolders);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/{folderId}")
    public ResponseEntity<Void> deleteSharedFolder(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "folderId") Long folderId,
            Authentication authentication) {

        ownershipGuard.checkUser(userId, authentication);
        sharedFolderService.deleteSharedFolder(userId, folderId);

        return ResponseEntity.noContent().build();
    }
}