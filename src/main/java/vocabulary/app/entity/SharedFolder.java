package vocabulary.app.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import vocabulary.app.dto.SharedFolderDTO;

import java.time.LocalDateTime;
import java.util.Date;

//folderId와 userId는 복합키
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedFolder {
    @EmbeddedId
    private SharedFolderId id;

//  직렬화 문제 해결하기 위해 FetchType.EAGER로 설정 하지만 N+1 문제 발생
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    
    @MapsId("folderId")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wordfolder_id")
    private WordFolder wordFolder;

    private LocalDateTime createDate;

    private Integer likes;

    public static SharedFolder create(SharedFolderDTO sharedFolderDTO, User user, WordFolder wordFolder){
        SharedFolder sharedFolder = new SharedFolder();
        sharedFolder.id = new SharedFolderId(sharedFolderDTO.getUserId(), sharedFolderDTO.getFolderId());
        sharedFolder.user = user;
        sharedFolder.wordFolder = wordFolder;
        sharedFolder.createDate = LocalDateTime.now();
        sharedFolder.likes=0;
        return sharedFolder;
    }
}

