package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.entity.Sentence;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;

import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    List<Sentence> findByWord(Word word);
    List<Sentence> findByUser(User user);
    List<Sentence> findByUserAndWord(User user, Word word);

    // 회원 탈퇴 시 사용
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Sentence S WHERE S.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
