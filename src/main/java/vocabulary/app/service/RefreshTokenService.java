package vocabulary.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.RefreshToken;
import vocabulary.app.entity.User;
import vocabulary.app.exception.InvalidRefreshTokenException;
import vocabulary.app.repository.RefreshTokenRepository;
import vocabulary.app.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // Refresh Token 유효 시간 (기본 14일)
    @Value("${jwt.refresh-token-validity-ms:1209600000}")
    private long refreshTokenValidityMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 로그인 성공 시 새 Refresh Token 발급
    @Transactional
    public RefreshToken issue(String username) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new InvalidRefreshTokenException("존재하지 않는 사용자입니다: " + username));
        return issue(user);
    }

    @Transactional
    public RefreshToken issue(User user) {
        RefreshToken refreshToken = new RefreshToken(
                generateTokenValue(),
                user,
                Instant.now().plusMillis(refreshTokenValidityMs)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    // Refresh Token 회전(rotation)
    // 한 번 사용된 토큰은 즉시 폐기하고 새 토큰을 발급한다.
    // 폐기된 토큰이 다시 들어오면 조회에 실패하므로 탈취 후 재사용을 막을 수 있다.
    @Transactional
    public RefreshToken rotate(String tokenValue) {
        RefreshToken saved = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("유효하지 않은 Refresh Token 입니다. 다시 로그인해 주세요."));

        // 만료됐으면 정리하고 재로그인 유도
        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw new InvalidRefreshTokenException("Refresh Token이 만료되었습니다. 다시 로그인해 주세요.");
        }

        User owner = saved.getUser();
        refreshTokenRepository.delete(saved);
        return issue(owner);
    }

    // 로그아웃: 전달받은 토큰만 폐기 (다른 기기의 로그인은 유지)
    @Transactional
    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(refreshTokenRepository::delete);
    }

    // 만료된 토큰 정리 (스케줄러에서 호출하거나 필요 시 수동 호출)
    @Transactional
    public int deleteExpiredTokens() {
        return refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }

    // 추측 불가능한 256비트 랜덤 문자열
    private String generateTokenValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
