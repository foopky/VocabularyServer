package vocabulary.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vocabulary.app.dto.LoginRequestDto;
import vocabulary.app.dto.LoginResponseDTO;
import vocabulary.app.dto.TokenRefreshRequestDTO;
import vocabulary.app.dto.TokenRefreshResponseDTO;
import vocabulary.app.entity.RefreshToken;
import vocabulary.app.entity.User;
import vocabulary.app.exception.InvalidRefreshTokenException;
import vocabulary.app.jwt.JwtTokenProvider;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.service.RefreshTokenService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {

        // 1. 사용자 인증 시도
        // UsernamePasswordAuthenticationToken을 생성하여 AuthenticationManager에 전달
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. 인증 성공 시, SecurityContext에 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. JWT Access Token 생성
        String jwt = jwtTokenProvider.createToken(authentication);

        // 4. Refresh Token 발급 (Access Token 만료 시 재발급에 사용)
        User user = userRepository.findByName(loginRequest.getUsername()).get();
        RefreshToken refreshToken = refreshTokenService.issue(user);

        // 5. 토큰을 응답 본문에 담아 클라이언트에게 반환
        return ResponseEntity.ok(new LoginResponseDTO(jwt, user.getId(), refreshToken.getToken()));
    }

    // Access Token 재발급
    // 프론트엔드는 401 + error=TOKEN_EXPIRED 응답을 받으면 이 API를 호출한다.
    // Refresh Token은 1회용이므로 응답으로 받은 새 Refresh Token으로 반드시 교체 저장해야 한다.
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody TokenRefreshRequestDTO request) {
        try {
            RefreshToken rotated = refreshTokenService.rotate(request.getRefreshToken());
            User user = rotated.getUser();

            String jwt = jwtTokenProvider.createToken(user.getName());

            return ResponseEntity.ok(new TokenRefreshResponseDTO(jwt, rotated.getToken(), user.getId()));
        } catch (InvalidRefreshTokenException e) {
            // Refresh Token까지 만료/폐기된 경우 -> 재로그인 필요
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "INVALID_REFRESH_TOKEN",
                    "message", e.getMessage()
            ));
        }
    }

    // 로그아웃: 전달받은 Refresh Token을 폐기한다.
    // Access Token은 만료 전까지 유효하므로 클라이언트에서도 함께 삭제해야 한다.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody TokenRefreshRequestDTO request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
