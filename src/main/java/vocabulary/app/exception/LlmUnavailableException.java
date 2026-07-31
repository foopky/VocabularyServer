package vocabulary.app.exception;

// LLM 호출 자체가 실패했을 때 (네트워크 오류, OpenAI 장애, API 키 문제, 타임아웃 …).
//
// "뜻을 못 찾았다"와 반드시 구분되어야 한다.
//  - 뜻을 못 찾음  -> 200 + {"meaning": null}  : 확장은 칸을 비워두고 사용자가 직접 입력한다
//  - 호출 실패     -> 503                      : 확장은 서버 장애로 취급한다
// 이걸 뭉뚱그려 200 으로 내리면 확장이 장애를 "이 단어는 뜻이 없구나"로 오해한다.
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmUnavailableException(String message) {
        super(message);
    }
}
