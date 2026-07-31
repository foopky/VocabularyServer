package vocabulary.app.exception;

import lombok.Getter;

// 사용자별 호출 제한을 넘겼을 때. GlobalExceptionHandler 가 429 + Retry-After 로 변환한다.
//
// LLM 비용이 드는 경로라서 필요하다. 사용자가 모달의 "+" 를 연타하면 그대로 요금이 된다.
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
