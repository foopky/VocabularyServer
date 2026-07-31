package vocabulary.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Setter
@Getter
public class WordFolder {
    @Schema(hidden = true)
    @Column(name="wordfolder_id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(hidden = true)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    private String name;

    private String language;

    // 이 폴더에 담긴 단어의 "뜻"이 어떤 언어로 적혀 있는지에 대한 기록.
    //
    // 제약이 아니라 기록이다. 폴더를 이 값으로 걸러내거나, 사용자가 넣는 뜻의 언어를 강제하지 않는다.
    // (/create_word_meaning 의 target_language 는 확장 팝업 설정에서 오므로 이 값과 다를 수 있고,
    //  서버는 둘을 대조하지 않는다.)
    // 기존 폴더는 값이 비어 있으며, 비어 있어도 동작에 영향이 없다.
    @Schema(description = "이 폴더의 뜻이 저장된 언어 코드 (ko, en …). 기록용이며 어떤 제약도 걸지 않는다.")
    @Column(name = "meaning_language")
    private String meaningLanguage;

//    @JsonIgnore
//    @ManyToMany(mappedBy = "wordFolders") // ManytoMany관계의 소유자를 지정 (이 때 wordFolders 변수이름 그대로 사용)
//    private List<Word> words = new ArrayList<>();

    public WordFolder addUser(User user)throws RuntimeException{
        if(this.user ==null)
            this.user = user;
        else throw new RuntimeException("이미 해당 WordFolder에 user가 포함되어 있습니다");
        return this;
    }
}
