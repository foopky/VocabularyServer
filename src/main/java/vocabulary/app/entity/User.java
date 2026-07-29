package vocabulary.app.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// "user"는 PostgreSQL 예약어이므로 테이블명을 명시해야 함
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    @Schema(hidden = true)
    private Long id;

    @Column(name="name")
    private String name;

    // BCrypt 해시는 어떤 응답에도 나가면 안 된다.
    // User가 중첩 직렬화되는 곳(Sentence.user, SharedFolder.user 등)까지 한 번에 막기 위해 엔티티에 건다.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // 기본값(ORDINAL)은 enum 상수 순서가 바뀌면 기존 데이터가 깨지므로 이름으로 저장
    @Enumerated(EnumType.STRING)
    private Role role;

    private String description;
}

