package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vocabulary.app.entity.Sentence;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;

import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    List<Sentence> findByWord(Word word);
    List<Sentence> findByUser(User user);
    List<Sentence> findByUserAndWord(User user, Word word);
}
