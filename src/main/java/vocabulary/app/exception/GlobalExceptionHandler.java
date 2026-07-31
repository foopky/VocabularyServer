package vocabulary.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.NoSuchElementException;

// 컨트롤러에서 처리하지 못한 예외를 JSON 응답으로 변환한다.
//
// 이게 없으면 예외가 컨테이너의 /error 디스패치까지 올라가는데,
// /error 는 JwtAuthenticationFilter를 타지 않아(OncePerRequestFilter는 ERROR 디스패치를 건너뛴다)
// 인증 정보가 없는 상태가 되고, 결국 서버 오류가 401로 둔갑한다.
// 프론트의 axios 인터셉터는 401을 세션 만료로 읽고 로그아웃시키므로 반드시 막아야 한다.
//
// ResponseEntityExceptionHandler를 상속해서 스프링 MVC 표준 예외(잘못된 JSON -> 400 등)의
// 원래 상태 코드는 그대로 유지한다.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 인증/인가 예외는 Spring Security의 핸들러(JwtAuthenticationEntryPoint 등)가 처리해야 한다.
    // 같은 인스턴스를 그대로 다시 던지면 스프링이 기본 처리로 넘겨준다.
    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    public void rethrowSecurityException(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", "요청한 데이터를 찾을 수 없습니다."));
    }

    // 잘못된 입력값(지원하지 않는 language, 영어 단어인데 pronunciation 누락 등).
    // 클라이언트가 고칠 수 있는 문제이므로 500이 아니라 400으로 내려준다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        // 프레임워크 내부에서 올라온 IllegalArgumentException까지 400으로 덮이면 진짜 버그가 안 보이므로 남긴다.
        logger.warn("잘못된 요청: {}", e.getMessage());

        // Map.of는 null 값을 허용하지 않으므로 메시지가 없는 경우를 대비한다.
        String message = (e.getMessage() != null) ? e.getMessage() : "잘못된 요청입니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", message));
    }

    // 사용자별 호출 제한 초과. Retry-After로 언제 다시 시도하면 되는지 알려준다.
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> handleRateLimit(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(Map.of("error", "TOO_MANY_REQUESTS", "message", e.getMessage()));
    }

    // LLM 호출 실패. 500이 아니라 503으로 내려서 "일시적인 장애, 다시 시도해도 된다"를 구분해준다.
    //
    // 여기서 200을 내리면 안 된다. 확장은 "뜻이 없음"(200 + meaning:null)과 "서버가 죽음"에
    // 서로 다르게 반응해야 하는데, 장애를 200으로 위장하면 사용자에게는 "이 단어는 뜻이 없다"로 보인다.
    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<?> handleLlmUnavailable(LlmUnavailableException e) {
        logger.error("LLM 호출 실패", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "LLM_UNAVAILABLE", "message", "뜻을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    // FK 제약 위반 등. 200으로 위장하지 않고 실패를 그대로 알린다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException e) {
        logger.error("데이터 무결성 제약 위반", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "DATA_INTEGRITY_VIOLATION", "message", "관련된 데이터가 남아 있어 처리할 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e) {
        logger.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류가 발생했습니다."));
    }
}
