package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SentenceDto {
    private Long wordId;
    private Long userId;
    private String sentence_text;
    private String style;
    private String meaning;
}
