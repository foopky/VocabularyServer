package vocabulary.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sentence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    //직렬화 문제 방지를 위해 JsonIgnore 추가
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id")
    @Schema(hidden = true)
    private Word word;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @Schema(hidden = true)
    private User user;

    private String sentence;
    private String style;
    private String meaning;
}
