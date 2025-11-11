package vocabulary.app.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "words")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// @NoArgsConstructor: 파라미터 없는 디퐅트생성자 @ALL: 모든 파라미터
// 빌더패턴: 자바 빈즈패턴의 확장, 불필요한 생성자 제거하고 정보를 다 받은 다음 객체를 생성
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    @Column(name = "word_id")
    private Long id;

    @Column(nullable = false)
    private String language;  // english or japanese
    @Column(nullable = false)
    private String pos;       // 품사
    @Column(nullable = false)
    private String word;      // 단어
    @Column(nullable = false)
    private String meaning;   // 뜻
    @Column(nullable = false)
    private boolean learned;  // 학습 여부

    // japanese attribute -> application 레벨에서 확인 / 비즈니스 로직 전략 패턴으로 분리
    private String kundoku;
    private String ondoku;

    // english attribute
    private String pronunciation;

    private String example;

//    @JsonIgnore
//    @ManyToMany
//    @JoinTable(name = "WordInFolder", joinColumns = @JoinColumn(name = "word_id"), inverseJoinColumns = @JoinColumn(name = "wordfolder_id",referencedColumnName = "wordfolder_id"))
//    private List<WordFolder> wordFolders = new ArrayList<>();

//    public void addWordFolder(WordFolder wordFolder){
//        this.wordFolders.add(wordFolder);
//        if(!wordFolder.getWords().contains(this)){
//            wordFolder.getWords().add(this);
//        }
//    }
//    public void removeWordFolder(WordFolder wordFolder) {
//        this.wordFolders.remove(wordFolder);
//        wordFolder.getWords().remove(this);
//    }
}