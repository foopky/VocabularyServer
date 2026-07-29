package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vocabulary.app.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // 비밀번호 변경(전체 기기 로그아웃) 및 회원 탈퇴 시 사용.
    // 탈퇴에서는 users 행을 지우기 전에 반드시 먼저 실행되어야 하므로 벌크 삭제로 둔다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken RT WHERE RT.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    // 만료된 토큰 정리용
    int deleteByExpiryDateBefore(Instant now);
}
