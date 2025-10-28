package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.entity.SharedFolderId;

public interface SharedFolderRepository extends JpaRepository<SharedFolder, SharedFolderId> {
    // SharedFolder 엔티티의 ID 필드는 'id' 객체 내부의 'folderId', 'userId' 필드를 사용해야 합니다.
// WordFolder 엔티티의 ID 필드는 'id'라고 가정합니다.
// User 엔티티의 ID 필드는 'id'라고 가정합니다.

    @Query("SELECT new vocabulary.app.dto.SharedFolderDTO(U.name, U.id, W.name, W.id) " +
            "FROM SharedFolder AS S " +
            "JOIN WordFolder AS W ON S.id.folderId = W.id " +
            "JOIN User AS U ON S.id.userId = U.id " +
            "WHERE S.id.userId = :user_id AND S.id.folderId = :folder_id")
    SharedFolderDTO getDetailedFolder(@Param("user_id") Long user_id, @Param("folder_id") Long folder_id);

    @Query("SELECT new vocabulary.app.dto.SharedFolderDTO(U.name, U.id, W.name, W.id) " +
            "FROM SharedFolder AS S " +
            "JOIN WordFolder AS W ON S.id.folderId = W.id " +
            "JOIN User AS U ON S.id.userId = U.id ")
    SharedFolderDTO[] getALLDetailedFolder();
}
