package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vocabulary.app.entity.Word;

import java.util.List;


public interface WordRepository extends JpaRepository<Word,Long> {
    List<Word> findByLanguage(String language);
    List<Word> findByLanguageAndLearned(String language, boolean learned);
}

