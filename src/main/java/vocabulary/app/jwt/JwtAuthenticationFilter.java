package vocabulary.app.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 인증 실패 사유를 EntryPoint로 전달하기 위한 request attribute 키
    public static final String AUTH_ERROR_ATTRIBUTE = "jwtAuthError";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 헤더에서 JWT 토큰 추출 (예: Authorization: Bearer <token>)
        String jwt = resolveToken(request);

        // 2. 토큰 유효성 검사 및 인증 처리
        if (jwt != null) {
            JwtTokenProvider.ValidationResult result = jwtTokenProvider.validate(jwt);

            if (result == JwtTokenProvider.ValidationResult.VALID) {
                // 토큰이 유효할 경우, 토큰으로부터 Authentication 객체를 얻어옴
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);

                // SecurityContext에 Authentication 객체를 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 만료/위조 사유를 남겨두면 JwtAuthenticationEntryPoint가 401 응답 본문에 담아준다.
                // 프론트는 TOKEN_EXPIRED를 보고 Refresh Token으로 재발급을 시도하면 된다.
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, result.name());
            }
        }
        // 예외를 잡아서 삼키면 안 된다.
        // 컨트롤러에서 터진 예외(FK 제약 위반 등)까지 여기서 먹어버리면
        // 응답이 200 + 빈 본문으로 나가서 실패한 요청이 성공처럼 보인다. (회원 탈퇴가 조용히 실패하던 원인)
        filterChain.doFilter(request, response); // 다음 필터로 요청 전달
    }

    // HTTP 요청 헤더에서 토큰을 추출하는 유틸리티 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거 후 순수 토큰 반환
        }
        return null;
    }
}
