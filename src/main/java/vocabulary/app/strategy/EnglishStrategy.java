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
    public ResponseEntity<Word> save(Word word){
        if (word.getPronunciation()==null){
            throw new IllegalArgumentException("English의 경우 pronunciation 필드에 String type이 입력되어야 합니다.");
        }
        else return new ResponseEntity<Word>(wordRepository.save(word),HttpStatus.OK);
    }

    @Transactional
    public void addWordToFolder(Long wordId, Long folderId){
        Word word = wordRepository.findById(wordId).orElseThrow();
        WordFolder folder= wordFolderRepository.findById(folderId).orElseThrow();
        WordInFolder wordInFolder = wordInFolderRepository.save(WordInFolder.create(word, folder));
    }

    @Transactional
    public WordFolder saveWordFolder(WordFolder wordFolder, Long userId){
        try {
            User user = userRepository.findById(userId).orElseThrow();
            return wordFolderRepository.save(wordFolder).addUser(user);
        } catch (Exception e){
            System.err.println("폴더에 유저 할당 중 예외 발생: " + e);
        }
        return null;
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
