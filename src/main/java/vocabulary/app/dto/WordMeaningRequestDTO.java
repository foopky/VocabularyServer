package vocabulary.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// POST /create_word_meaning 요청 본문.
//
// base_form 과 word 를 나눠 받는 이유: 자막에는 활용형(掛け)이 뜨는데, 활용형으로 사전을 찾으면
// 못 찾거나 엉뚱한 게 나온다. 조회는 사전형(掛ける)으로 해야 한다.
@Schema(description = "단어 뜻 자동 입력 요청")
public record WordMeaningRequestDTO(

        @Schema(description = "사전형. 조회·캐시의 기준 키", example = "掛ける", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("base_form") String baseForm,

        @Schema(description = "자막에 실제로 나온 활용형 (참고용)", example = "掛け", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("word") String word,

        @Schema(description = "가타카나 읽기. 동형이의어 판별에 필수", example = "カケ", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("reading") String reading,

        @Schema(description = "품사", example = "動詞")
        @JsonProperty("pos") String pos,

        @Schema(description = "품사 세분류", example = "自立")
        @JsonProperty("pos_detail") String posDetail,

        @Schema(description = "뜻을 받을 언어 코드", example = "ko", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("target_language") String targetLanguage
) {
}
