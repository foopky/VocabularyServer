package vocabulary.app.jwt;

import io.jsonwebtoken.io.IOException;
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

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 헤더에서 JWT 토큰 추출 (예: Authorization: Bearer <token>)
        String jwt = resolveToken(request);

        // 2. 토큰 유효성 검사 및 인증 처리
        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            // 토큰이 유효할 경우, 토큰으로부터 Authentication 객체를 얻어옴
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);

            // SecurityContext에 Authentication 객체를 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        try {
            filterChain.doFilter(request, response); // 다음 필터로 요청 전달
        } catch(Exception e){
            System.err.println("다음 필터로 요청 전달 중 예외 발생: "+ e);
        }
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
