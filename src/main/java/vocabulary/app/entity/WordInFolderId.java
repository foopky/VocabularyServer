package vocabulary.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class WordInFolderId {
    @Column(name = "word_id")
    private Long wordId;
    @Column(name = "folder_id")
    private Long folderId;
}
