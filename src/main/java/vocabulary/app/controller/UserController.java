package vocabulary.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.ChangePasswordRequestDTO;
import vocabulary.app.dto.UserResponseDTO;
import vocabulary.app.dto.UserSignupRequestDTO;
import vocabulary.app.dto.UserUpdateRequestDTO;
import vocabulary.app.entity.User;
import vocabulary.app.service.UserService;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserSignupRequestDTO request) {
        try {
            // role은 요청 본문에서 받지 않는다. 서버가 항상 USER로 고정한다.
            User savedUser = userService.saveUser(request);

            // HTTP 201 Created 상태와 함께 저장된 User 정보를 반환 (비밀번호 제외)
            return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponseDTO(savedUser));
        } catch (Exception e) {
            // 가입 중 예외 발생(이미 존재하는 UserId 등)
            System.err.println("Exception: " + e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable(name = "userId") Long userId,
                                     Authentication authentication) {
        try {
            User user = userService.getUser(userId);
            if (isNotOwner(user, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(new UserResponseDTO(user));
        } catch (NoSuchElementException e) {
            // 해당 ID의 User가 없으면 404 Not Found 반환
            return ResponseEntity.notFound().build();
        }
    }

    // 프로필 수정. 비밀번호는 받지 않는다(PUT /{userId}/password 사용).
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable(name = "userId") Long userId,
                                        @RequestBody UserUpdateRequestDTO request,
                                        Authentication authentication) {
        try {
            User user = userService.getUser(userId);
            if (isNotOwner(user, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            User savedUser = userService.editUser(userId, request);
            return ResponseEntity.ok(new UserResponseDTO(savedUser));

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 비밀번호 변경.
    // 현재 비밀번호가 틀린 경우는 401이 아니라 400으로 응답해야 한다.
    // 프론트의 axios 인터셉터가 401을 "세션 만료"로 해석해 토큰 재발급 후 /login으로 튕기기 때문.
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changePassword(@PathVariable(name = "userId") Long userId,
                                            @RequestBody ChangePasswordRequestDTO request,
                                            Authentication authentication) {
        try {
            User user = userService.getUser(userId);
            if (isNotOwner(user, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UserService.PasswordChangeResult result =
                    userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

            if (result == UserService.PasswordChangeResult.SUCCESS) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", result.name()));

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable(name = "userId") Long userId,
                                           Authentication authentication) {
        try {
            User user = userService.getUser(userId);
            if (isNotOwner(user, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            userService.deleteUser(userId);

            // HTTP 204 No Content 반환 (삭제 성공 시 본문이 없음)
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 본인 확인. JWT의 subject가 사용자 이름이므로 이름으로 비교한다.
    // (토큰만 유효하면 남의 계정을 조회/수정/삭제할 수 있었던 문제를 막는다)
    private boolean isNotOwner(User user, Authentication authentication) {
        return authentication == null || !user.getName().equals(authentication.getName());
    }
}
