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
