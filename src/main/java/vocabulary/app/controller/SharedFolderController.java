package vocabulary.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.service.SharedFolderService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/sharedFolder")
public class SharedFolderController {

    private final SharedFolderService sharedFolderService;

    public SharedFolderController(SharedFolderService sharedFolderService) {
        this.sharedFolderService = sharedFolderService;
    }

    @PostMapping
    public ResponseEntity<SharedFolder> addSharedFolder(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name= "folderId") Long folderId) {

        SharedFolder newSharedFolder = sharedFolderService.addSharedFolder(userId, folderId);

        // HTTP 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(newSharedFolder);
    }

    @GetMapping
    public ResponseEntity<SharedFolderDTO[]> getSharedFolders() {
        SharedFolderDTO[] sharedFolders = sharedFolderService.getSharedFolders();
        return ResponseEntity.ok(sharedFolders);
    }

    @GetMapping("/{userId}/{folderId}")
    public ResponseEntity<SharedFolderDTO> getDetailedSharedFolder(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "folderId") Long folderId) {
        try {
            SharedFolderDTO detailedFolder = sharedFolderService.getDetailed(userId, folderId);
            return ResponseEntity.ok(detailedFolder);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/{folderId}")
    public ResponseEntity<Void> deleteSharedFolder(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "folderId") Long folderId) {

        sharedFolderService.deleteSharedFolder(userId, folderId);

        return ResponseEntity.noContent().build();
    }
}