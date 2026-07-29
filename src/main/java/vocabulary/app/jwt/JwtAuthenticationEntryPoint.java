package vocabulary.app.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

// 인증되지 않은 요청에 대해 403이 아닌 401을 내려준다.
// 만료된 토큰이면 error=TOKEN_EXPIRED 를 함께 내려주어
// 프론트엔드가 Refresh Token으로 재발급할지, 재로그인을 유도할지 판단할 수 있게 한다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Object authError = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);

        String error;
        String message;
        if (JwtTokenProvider.ValidationResult.EXPIRED.name().equals(authError)) {
            error = "TOKEN_EXPIRED";
            message = "Access Token이 만료되었습니다. Refresh Token으로 재발급해 주세요.";
        } else if (JwtTokenProvider.ValidationResult.INVALID.name().equals(authError)) {
            error = "INVALID_TOKEN";
            message = "유효하지 않은 Access Token 입니다.";
        } else {
            error = "UNAUTHORIZED";
            message = "인증이 필요합니다.";
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", error,
                "message", message
        ));
    }
}
