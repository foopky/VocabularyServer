package vocabulary.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// POST /create_word_meaning 응답.
//
// 뜻을 판정하지 못하면 meaning 이 null 인 200 을 내려준다.
// 서비스 장애·인증 실패는 비-2xx 로 구분한다 — 확장이 "뜻이 없음" 과 "서버가 죽음" 에
// 서로 다르게 반응해야 하기 때문이다.
@Schema(description = "단어 뜻 자동 입력 응답")
public record WordMeaningResponseDTO(

        @Schema(description = "쉼표로 구분한 뜻 1~3개. 판정하지 못하면 null", example = "걸다, 매달다", nullable = true)
        String meaning
) {
    public static WordMeaningResponseDTO of(String meaning) {
        return new WordMeaningResponseDTO(meaning);
    }
}
