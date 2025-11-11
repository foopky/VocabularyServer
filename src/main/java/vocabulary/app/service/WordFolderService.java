package vocabulary.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.repository.WordFolderRepository;
import vocabulary.app.repository.WordInFolderRepository;
import vocabulary.app.strategy.WordStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WordFolderService {

    private final Map<String, WordStrategy> strategies;
    private final WordFolderRepository wordFolderRepository;
    private final WordInFolderRepository wordInFolderRepository;

    public WordFolderService(Map<String, WordStrategy> strategies, WordFolderRepository wordFolderRepository, WordInFolderRepository wordInFolderRepository){
        this.strategies = strategies;
        this.wordFolderRepository = wordFolderRepository;
        this.wordInFolderRepository = wordInFolderRepository;
    }

    private WordStrategy getStrategy(String language) {
        WordStrategy strategy = strategies.get(language.toLowerCase()); // "english" / "japanese"
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 언어: " + language);
        }
        return strategy;
    }

    // WordInFolder에 맞춰 로직 수정 필요
    @Transactional
    public Object saveWordFolder(String language, WordFolder wordFolder, Long userId){return getStrategy(language).saveWordFolder(wordFolder, userId);}

    // WordInFolder에 맞춰 로직 수정 필요
    @Transactional
    public void addWordToFolder(String language, Long wordId, Long folderId){ getStrategy(language).addWordToFolder(wordId,folderId); }

    @Transactional
    public List<Word> getAllWordsInFolder(Long folderId){
        return wordInFolderRepository.getAllWordsOnFolder(folderId);
    }

    @Transactional
    public List<WordFolder> getAllWordFolder(String language) { return getStrategy(language).getAllWordFolder();}

    @Transactional
    public void deleteFolder(Long folderId){
        wordFolderRepository.deleteById(folderId);
    }
}
