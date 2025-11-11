package vocabulary.app.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import vocabulary.app.dto.SharedFolderDTO;

import java.time.LocalDateTime;
import java.util.Date;

//folderId와 userId는 복합키
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedFolder {
    @EmbeddedId
    private SharedFolderId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("folderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wordfolder_id")
    private WordFolder wordFolder;

    private LocalDateTime createDate;

    @ColumnDefault("0")
    private Integer likes;

    public static SharedFolder create(SharedFolderDTO sharedFolderDTO, User user, WordFolder wordFolder){
        SharedFolder sharedFolder = new SharedFolder();
        sharedFolder.id = new SharedFolderId(sharedFolderDTO.getUserId(), sharedFolderDTO.getFolderId());
        sharedFolder.user = user;
        sharedFolder.wordFolder = wordFolder;
        return sharedFolder;
    }


}

