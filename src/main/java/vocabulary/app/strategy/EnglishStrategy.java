package vocabulary.app.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.entity.WordInFolder;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordFolderRepository;
import vocabulary.app.repository.WordInFolderRepository;
import vocabulary.app.repository.WordRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Component("english")
public class EnglishStrategy implements WordStrategy {
    private final WordRepository wordRepository;
    private final WordFolderRepository wordFolderRepository;
    private final UserRepository userRepository;
    private final WordInFolderRepository wordInFolderRepository;

    @Autowired
    public EnglishStrategy(
            WordRepository wordRepository,
            WordFolderRepository wordFolderRepository,
            UserRepository userRepository,
            WordInFolderRepository wordInFolderRepository){
        this.wordRepository = wordRepository;
        this.wordFolderRepository = wordFolderRepository;
        this.userRepository = userRepository;
        this.wordInFolderRepository = wordInFolderRepository;
    }

    @Transactional
    public List<Word> getAll(){
       return wordRepository.findByLanguage("english");
    }

    @Transactional
    public List<WordFolder> getAllWordFolder(){
        return wordFolderRepository.findByLanguage("english");
    }

    @Transactional
    public List<Word> getByLearned(boolean learned){
        return wordRepository.findByLanguageAndLearned("english", learned);
    }

    @Transactional
    public Word save(Word word){
        if (word.getPronunciation()==null){
            throw new IllegalArgumentException("English의 경우 pronunciation 필드에 String type이 입력되어야 합니다.");
        }
        else return wordRepository.save(word);
    }

    @Transactional
    public void addWordToFolder(Long wordId, Long folderId){
        Word word = wordRepository.findById(wordId).orElseThrow();
        WordFolder folder= wordFolderRepository.findById(folderId).orElseThrow();
        WordInFolder wordInFolder = wordInFolderRepository.save(WordInFolder.create(word, folder));
    }

    // 예외를 삼키고 null을 반환하면 컨트롤러가 200 + 빈 본문을 내보내 실패가 성공처럼 보인다.
    // 또 save를 먼저 하면 user 할당이 실패했을 때 user_id가 비어 있는 폴더가 그대로 남는다.
    // 그래서 user를 먼저 붙이고 저장한다.
    @Transactional
    public WordFolder saveWordFolder(WordFolder wordFolder, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));
        return wordFolderRepository.save(wordFolder.addUser(user));
    }

    @Transactional
    public void delete(Long id){
        wordRepository.deleteById(id);
    }

//    @Transactional
//    public void deleteFolder(Long folderId){
//        wordFolderRepository.deleteById(folderId);
//    }
}
