package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.entity.User;
import vocabulary.app.entity.WordFolder;

import java.util.List;

public interface WordFolderRepository extends JpaRepository<WordFolder, Long> {
    List<WordFolder> findByLanguage(String language);
    List<WordFolder> findByUser(User user);

    // 회원 탈퇴 시 사용. 실행 순서를 보장해야 하므로 파생 삭제 대신 벌크 삭제를 쓴다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WordFolder W WHERE W.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
