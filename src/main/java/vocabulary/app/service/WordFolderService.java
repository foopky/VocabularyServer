package vocabulary.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.repository.WordFolderRepository;
import vocabulary.app.strategy.WordStrategy;

import java.util.List;
import java.util.Map;

@Service
public class WordFolderService {

    private final Map<String, WordStrategy> strategies;
    private final WordFolderRepository wordFolderRepository;

    public WordFolderService(Map<String, WordStrategy> strategies, WordFolderRepository wordFolderRepository){
        this.strategies = strategies;
        this.wordFolderRepository = wordFolderRepository;
    }

    private WordStrategy getStrategy(String language) {
        WordStrategy strategy = strategies.get(language.toLowerCase()); // "english" / "japanese"
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 언어: " + language);
        }
        return strategy;
    }

    @Transactional
    public Object saveWordFolder(String language, WordFolder wordFolder, Long userId){return getStrategy(language).saveWordFolder(wordFolder, userId);}

    @Transactional
    public void addWordToFolder(String language, Long wordId, Long folderId){ getStrategy(language).addWordToFolder(wordId,folderId); }

    @Transactional
    public List<Word> getAllWordsInFolder(Long folderId){
        WordFolder folder = wordFolderRepository.findById(folderId).orElseThrow();
        return folder.getWords();
    }

    @Transactional
    public List<WordFolder> getAllWordFolder(String language) { return getStrategy(language).getAllWordFolder();}

    @Transactional
    public void deleteFolder(Long folderId){
        wordFolderRepository.deleteById(folderId);
    }
}
