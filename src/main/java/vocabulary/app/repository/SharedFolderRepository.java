package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.entity.SharedFolderId;
import vocabulary.app.entity.User;

import java.util.List;

public interface SharedFolderRepository extends JpaRepository<SharedFolder, SharedFolderId> {
    List<SharedFolder> findByUser(User user);

    // 공유 게시판에 올라온 건수. 0보다 크면 주인이 아니어도 단어를 열람할 수 있다.
    @Query("SELECT COUNT(S) FROM SharedFolder S WHERE S.id.folderId = :folderId")
    long countByFolderId(@Param("folderId") Long folderId);

    // 회원 탈퇴 시 사용: 이 사용자가 직접 공유한 글
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SharedFolder S WHERE S.id.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    // 회원 탈퇴 시 사용: 이 사용자의 폴더를 가리키는 공유 글(공유한 사람이 남이어도 폴더가 사라지므로 함께 지운다)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SharedFolder S " +
            "WHERE S.id.folderId IN (SELECT W.id FROM WordFolder AS W WHERE W.user.id = :userId)")
    int deleteAllByFolderOwnerId(@Param("userId") Long userId);
    // SharedFolder 엔티티의 ID 필드는 'id' 객체 내부의 'folderId', 'userId' 필드를 사용해야 합니다.
// WordFolder 엔티티의 ID 필드는 'id'라고 가정합니다.
// User 엔티티의 ID 필드는 'id'라고 가정합니다.

//    @Query("SELECT new vocabulary.app.dto.SharedFolderDTO(U.name, U.id, W.name, W.id) " +
//            "FROM SharedFolder AS S " +
//            "JOIN WordFolder AS W ON S.id.folderId = W.id " +
//            "JOIN User AS U ON S.id.userId = U.id " +
//            "WHERE S.id.userId = :user_id AND S.id.folderId = :folder_id")
//    SharedFolderDTO getDetailedFolder(@Param("user_id") Long user_id, @Param("folder_id") Long folder_id);
//
//    @Query("SELECT new vocabulary.app.dto.SharedFolderDTO(U.name, U.id, W.name, W.id) " +
//            "FROM SharedFolder AS S " +
//            "JOIN WordFolder AS W ON S.id.folderId = W.id " +
//            "JOIN User AS U ON S.id.userId = U.id ")
//    SharedFolderDTO[] getALLDetailedFolder();
}
