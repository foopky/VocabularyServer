package vocabulary.app.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vocabulary.app.service.RefreshTokenService;

// 만료된 Refresh Token은 재발급에 쓰이지 않으므로 주기적으로 삭제한다.
// 삭제 조건이 "만료 시각이 지난 것"뿐이라 여러 인스턴스에서 동시에 실행돼도 안전하다.
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final RefreshTokenService refreshTokenService;

    // 기본값: 매일 새벽 4시 (cron 값을 "-"로 두면 스케줄러가 비활성화된다)
    @Scheduled(cron = "${jwt.refresh-token-cleanup-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void cleanupExpiredTokens() {
        try {
            int deleted = refreshTokenService.deleteExpiredTokens();
            if (deleted > 0) {
                logger.info("만료된 Refresh Token {}건을 삭제했습니다.", deleted);
            }
        } catch (Exception e) {
            // 스케줄러에서 예외가 나가면 이후 실행이 멈출 수 있으므로 로그만 남기고 넘어간다.
            logger.error("만료된 Refresh Token 정리 중 예외 발생", e);
        }
    }
}
