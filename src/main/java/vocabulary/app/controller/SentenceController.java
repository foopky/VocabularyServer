package vocabulary.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.SentenceDto;
import vocabulary.app.entity.Sentence;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;
import vocabulary.app.repository.SentenceRepository;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordRepository;
import vocabulary.app.security.OwnershipGuard;

import java.util.List;

@RestController
@RequestMapping("/sentence")
public class SentenceController {
    private final SentenceRepository sentenceRepository;
    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final OwnershipGuard ownershipGuard;
    public SentenceController(SentenceRepository sentenceRepository,
                              UserRepository userRepository,
                              WordRepository wordRepository,
                              OwnershipGuard ownershipGuard) {
        this.sentenceRepository = sentenceRepository;
        this.userRepository = userRepository;
        this.wordRepository = wordRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @GetMapping("/by-user-and-word")
    public ResponseEntity<List<Sentence>> getSentenceByUserAndWord(@RequestParam Long userId, @RequestParam Long wordId,
                                                                   Authentication authentication) {
        ownershipGuard.checkUser(userId, authentication);
        User user = userRepository.findById(userId).orElseThrow();
        Word word = wordRepository.findById(wordId).orElseThrow();
        return ResponseEntity.ok(sentenceRepository.findByUserAndWord(user, word));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Sentence>> getSentenceByUserId(@PathVariable Long userId,
                                                              Authentication authentication){
        ownershipGuard.checkUser(userId, authentication);
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(sentenceRepository.findByUser(user));
    }


    @PostMapping
    public ResponseEntity<Sentence> saveSentence(@RequestBody SentenceDto req,
                                                 Authentication authentication){
        // 요청 본문의 userId를 그대로 믿으면 남의 이름으로 예문을 만들 수 있다.
        ownershipGuard.checkUser(req.getUserId(), authentication);

        Word word = wordRepository.findById(req.getWordId()).orElseThrow();
        User user = userRepository.findById(req.getUserId()).orElseThrow();
        Sentence sentence = Sentence.builder().
                sentence(req.getSentence_text()).
                style(req.getStyle()).
                word(word).
                user(user).
                meaning(req.getMeaning()).
                build();
        return ResponseEntity.ok(sentenceRepository.save(sentence));
    }

    // @PathVariable이 없으면 sentenceId가 경로가 아닌 쿼리 파라미터로 해석되어 항상 null이 되고,
    // deleteById(null)로 500이 났다.
    @DeleteMapping("/{sentenceId}")
    public ResponseEntity<?> deleteSentence(@PathVariable("sentenceId") Long sentenceId,
                                            Authentication authentication){
        ownershipGuard.checkSentenceOwner(sentenceId, authentication);
        sentenceRepository.deleteById(sentenceId);
        return ResponseEntity.noContent().build();
    }
}
