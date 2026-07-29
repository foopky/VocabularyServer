package vocabulary.app.exception;

// Refresh Token이 없거나, 만료됐거나, 이미 사용되어 폐기된 경우
// 클라이언트는 이 예외(401)를 받으면 재로그인을 유도해야 한다.
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
