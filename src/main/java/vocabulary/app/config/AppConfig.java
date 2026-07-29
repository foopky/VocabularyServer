package vocabulary.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAspectJAutoProxy
// 만료된 Refresh Token 정리(RefreshTokenCleanupScheduler)를 위해 스케줄링 활성화
@EnableScheduling
public class AppConfig {
}
