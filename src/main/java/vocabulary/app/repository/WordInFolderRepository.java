package vocabulary.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordInFolder;
import vocabulary.app.entity.WordInFolderId;

import java.util.List;

public interface WordInFolderRepository extends JpaRepository<WordInFolder, WordInFolderId> {
    // 폴더에 담긴 단어 조회.
    // 정렬 조건(sort=word 등)이 Word의 필드로 그대로 해석되도록 Word를 조회 대상으로 두고
    // 폴더 조건은 서브쿼리로 처리한다. (JOIN 형태로 두면 정렬 기준이 WordInFolder에 걸린다)
    @Query(value = "SELECT W FROM words AS W " +
            "WHERE W.id IN (SELECT WIF.id.wordId FROM WordInFolder AS WIF WHERE WIF.id.folderId = :folderId)",
            countQuery = "SELECT COUNT(W) FROM words AS W " +
                    "WHERE W.id IN (SELECT WIF.id.wordId FROM WordInFolder AS WIF WHERE WIF.id.folderId = :folderId)")
    Page<Word> getAllWordsOnFolder(@Param("folderId") Long folderId, Pageable pageable);

    @Override
    void deleteById(WordInFolderId wordInFolderId);

    @Query("SELECT WIF FROM WordInFolder AS WIF " +
            "WHERE WIF.id.wordId = :wordId")
    List<WordInFolder> getAllWordInFolderOnWordId(@Param("wordId") Long wordId);

    // 회원 탈퇴 시 사용: 탈퇴자의 폴더에 담긴 단어 매핑을 먼저 지워야 폴더를 지울 수 있다.
    // (words 테이블 자체는 공용이므로 건드리지 않는다)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WordInFolder AS WIF " +
            "WHERE WIF.id.folderId IN (SELECT W.id FROM WordFolder AS W WHERE W.user.id = :userId)")
    int deleteAllByFolderOwnerId(@Param("userId") Long userId);
}
