package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vocabulary.app.entity.User;
import vocabulary.app.entity.WordFolder;

import java.util.List;

public interface WordFolderRepository extends JpaRepository<WordFolder, Long> {
    List<WordFolder> findByLanguage(String language);
    List<WordFolder> findByUser(User user);
}
