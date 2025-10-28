package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SharedFolderDTO {
    private String userName;
    private Long userId;
    private String wordName;
    private Long wordId;
}
