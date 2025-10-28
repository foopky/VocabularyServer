package vocabulary.app.strategy;

import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;

import java.util.List;

public interface WordStrategy {

    public List<Word> getAll();
    public List<Word> getByLearned(boolean learned);
    public Object save(Word word);
    public void delete(Long id);

    public List<WordFolder> getAllWordFolder();
    public WordFolder saveWordFolder(WordFolder wordFolder, Long userId);
    public void addWordToFolder(Long wordId, Long folderId);
//    public void deleteFolder(Long folderId);
}
