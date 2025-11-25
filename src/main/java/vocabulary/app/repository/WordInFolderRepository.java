package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordInFolder;
import vocabulary.app.entity.WordInFolderId;

import java.util.List;

public interface WordInFolderRepository extends JpaRepository<WordInFolder, WordInFolderId> {
    @Query("SELECT W FROM WordInFolder AS WIF " +
            "JOIN WIF.word AS W " +
            "WHERE WIF.id.folderId = :folderId")
    List<Word> getAllWordsOnFolder(@Param("folderId") Long folderId);

    @Override
    void deleteById(WordInFolderId wordInFolderId);

    @Query("SELECT WIF FROM WordInFolder AS WIF " +
            "WHERE WIF.id.wordId = :wordId")
    List<WordInFolder> getAllWordInFolderOnWordId(@Param("wordId") Long wordId);
}
