package vocabulary.app.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SharedFolderId implements Serializable {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "folder_id")
    private Long folderId;
}