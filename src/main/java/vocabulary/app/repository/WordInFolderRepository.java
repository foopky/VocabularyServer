package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordInFolder;

import java.util.List;

public interface WordInFolderRepository extends JpaRepository<WordInFolder, Long> {
    @Query("SELECT W FROM WordInFolder AS WIF " +
            "JOIN WIF.word AS W " +
            "WHERE WIF.id.folderId = :folderId")
    List<Word> getAllWordsOnFolder(@Param("folderId") Long folderId);
}
