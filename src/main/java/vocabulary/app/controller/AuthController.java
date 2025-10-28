package vocabulary.app.controller;

import lombok.RequiredArgsConstructor;
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
import vocabulary.app.jwt.JwtTokenProvider;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto loginRequest) {

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

        // 4. 토큰을 응답 본문이나 헤더에 담아 클라이언트에게 반환
        // 실제 서비스에서는 Refresh Token도 함께 발급하고 관리합니다.
        return ResponseEntity.ok(jwt);
    }

    // DTO 생략: LoginRequestDto { String username, String password; }
}