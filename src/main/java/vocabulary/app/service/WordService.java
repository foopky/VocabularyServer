package vocabulary.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordInFolderId;
import vocabulary.app.repository.WordInFolderRepository;
import vocabulary.app.repository.WordRepository;
import vocabulary.app.strategy.WordStrategy;

import java.util.List;
import java.util.Map;

@Service
public class WordService {

    private final Map<String, WordStrategy> strategies;
    private final WordRepository wordRepository;
    private final WordInFolderRepository wordInFolderRepository;

    // Spring이 모든 WordStrategy 빈을 Map으로 주입해줌 (key = Bean 이름)
    public WordService(
            Map<String, WordStrategy> strategies,
            WordRepository wordRepository,
            WordInFolderRepository wordInFolderRepository) {
        this.strategies = strategies;
        this.wordRepository = wordRepository;
        this.wordInFolderRepository = wordInFolderRepository;
    }

    // 언어에 따라 strategy 패턴 이용 -> 폴더 저장도 strategy 패턴 적용 필요 o
    // -> 워드를 저장할 때 폴더를 지정 필요 -> 폴더를 언어에 대해 분리 -> 폴더 속성에 language 필요
    private WordStrategy getStrategy(String language) {
        WordStrategy strategy = strategies.get(language.toLowerCase()); // "english" / "japanese"
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 언어: " + language);
        }
        return strategy;
    }

    @Transactional(readOnly = true)
    public List<Word> getAll(String language) {
        return getStrategy(language).getAll();
    }


    @Transactional(readOnly = true)
    public List<Word> getByLearned(String language, boolean learned) {
        return getStrategy(language).getByLearned(learned);
    }

    @Transactional
    public Object save(String language, Word word) {
        return getStrategy(language).save(word);
    }


    @Transactional
    public void delete(WordInFolderId wordInFolderId) {
        wordInFolderRepository.deleteById(wordInFolderId);
//      폴더와 연결되는 단어 없을 시 완전히 단어자체를 삭제.
        if(wordInFolderRepository.getAllWordInFolderOnWordId(wordInFolderId.getWordId()).isEmpty()){
            wordRepository.deleteById(wordInFolderId.getWordId());
        }
    }
}