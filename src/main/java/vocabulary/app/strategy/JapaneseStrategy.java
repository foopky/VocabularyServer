package vocabulary.app.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordFolderRepository;
import vocabulary.app.repository.WordRepository;

import java.util.List;

@Component("japanese")
public class JapaneseStrategy implements WordStrategy{
    private final WordRepository wordRepository;
    private final WordFolderRepository wordFolderRepository;
    private final UserRepository userRepository;

    @Autowired
    public JapaneseStrategy(WordRepository wordRepository, WordFolderRepository wordFolderRepository, UserRepository userRepository){
        this.wordRepository = wordRepository;
        this.wordFolderRepository = wordFolderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<Word> getAll(){
        return wordRepository.findByLanguage("japanese");
    }

    @Transactional
    public List<WordFolder> getAllWordFolder(){
        return wordFolderRepository.findByLanguage("japanese");
    }

    @Transactional
    public List<Word> getByLearned(boolean learned){
        return wordRepository.findByLanguageAndLearned("japanese", learned);
    }

    @Transactional
    public ResponseEntity<Word> save(Word word){
        if (word.getOndoku()==null && word.getKundoku()==null) {
            System.out.println("Exception occurs at JapaneseStrategy.");
            throw new IllegalArgumentException("Japanese의 경우 ondoku또는 kundoku 필드에 String type이 입력되어야 합니다.");
        }
        else return new ResponseEntity<Word>(wordRepository.save(word), HttpStatus.OK);
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
    public void addWordToFolder(Long wordId, Long folderId){
        Word word = wordRepository.findById(wordId).orElseThrow();
        WordFolder folder= wordFolderRepository.findById(folderId).orElseThrow();

        word.addWordFolder(folder);
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
