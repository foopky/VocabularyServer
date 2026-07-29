package vocabulary.app.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Access Token 재발급에 사용하는 Refresh Token
// 탈취 시 폐기(revoke)가 가능해야 하므로 JWT가 아닌 불투명(opaque) 랜덤 문자열을 DB에 저장한다.
@Entity
@Table(
        name = "refresh_tokens",
        indexes = @Index(name = "idx_refresh_tokens_token", columnList = "token")
)
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    @Schema(hidden = true)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    // 토큰 주인. 재발급 시 이 사용자로 새 Access Token을 만든다.
    // 토큰을 조회하는 시점에는 항상 사용자 정보가 필요하므로 EAGER로 둔다.
    // (LAZY로 두면 트랜잭션 밖인 컨트롤러에서 LazyInitializationException이 발생한다)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RefreshToken(String token, User user, Instant expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
