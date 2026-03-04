package vocabulary.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.SentenceDto;
import vocabulary.app.entity.Sentence;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;
import vocabulary.app.repository.SentenceRepository;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordRepository;

import java.util.List;

@RestController
@RequestMapping("/sentence")
public class SentenceController {
    private final SentenceRepository sentenceRepository;
    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    public SentenceController(SentenceRepository sentenceRepository,
                              UserRepository userRepository,
                              WordRepository wordRepository) {
        this.sentenceRepository = sentenceRepository;
        this.userRepository = userRepository;
        this.wordRepository = wordRepository;
    }

    @GetMapping("/by-user-and-word")
    public ResponseEntity<List<Sentence>> getSentenceByUserAndWord(@RequestParam Long userId, @RequestParam Long wordId) {
        User user = userRepository.findById(userId).orElseThrow();
        Word word = wordRepository.findById(wordId).orElseThrow();
        return ResponseEntity.ok(sentenceRepository.findByUserAndWord(user, word));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Sentence>> getSentenceByUserId(@PathVariable Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(sentenceRepository.findByUser(user));
    }


    @PostMapping
    public ResponseEntity<Sentence> saveSentence(@RequestBody SentenceDto req){
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

    @DeleteMapping("/{sentenceId}")
    public ResponseEntity<?> deleteSentence(Long sentenceId){
        sentenceRepository.deleteById(sentenceId);
        return ResponseEntity.noContent().build();
    }
}
