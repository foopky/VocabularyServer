package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SharedFolderDTO {
    private Long userId;
    private Long folderId;
    private String name;
    private String language;
}
