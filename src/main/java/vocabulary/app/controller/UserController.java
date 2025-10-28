package vocabulary.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.entity.User;
import vocabulary.app.service.UserService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);

        // HTTP 201 Created 상태와 함께 저장된 User 객체를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable(name = "userId") Long userId) {
        try {
            User user = userService.getUser(userId);
            return ResponseEntity.ok(user);
        } catch (NoSuchElementException e) {
            // 해당 ID의 User가 없으면 404 Not Found 반환
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable(name = "userId") Long userId, @RequestBody User updatedUser) {
        try {
            User existingUser = userService.getUser(userId);
            User savedUser = userService.saveUser(existingUser);

            return ResponseEntity.ok(savedUser);

        } catch (NoSuchElementException e) {
            // 수정 대상 User가 없으면 404 Not Found 반환
            return ResponseEntity.notFound().build();
        }
    }


    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable(name = "userId") Long userId) {
        userService.deleteUser(userId);

        // HTTP 204 No Content 반환 (삭제 성공 시 본문이 없음)
        return ResponseEntity.noContent().build();
    }
}