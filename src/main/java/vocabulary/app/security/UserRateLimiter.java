package vocabulary.app.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vocabulary.app.exception.RateLimitExceededException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// 사용자별 고정 윈도 호출 제한.
//
// LLM 비용이 드는 경로(/create_word_meaning)를 보호한다. 사용자가 모달의 "+" 를 연타할 수 있는데,
// 클릭 한 번이 그대로 OpenAI 요금이 된다.
//
// 인스턴스 메모리에만 둔다. Redis 같은 외부 저장소가 이 프로젝트에 없고, 서버가 여러 대로 늘어나면
// 사용자당 한도가 인스턴스 수만큼 늘어날 뿐 제한 자체가 무너지지는 않는다.
// (남용 방지가 목적이지 정확한 과금 통제가 목적이 아니다.)
@Slf4j
@Component
public class UserRateLimiter {

    private final int capacity;
    private final long windowMs;

    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    // 쓰지 않는 사용자 항목이 앱 수명 내내 쌓이지 않도록 가끔 청소한다.
    private final AtomicLong lastSweepMs = new AtomicLong(System.currentTimeMillis());

    public UserRateLimiter(
            @Value("${wordmeaning.rate-limit.capacity:20}") int capacity,
            @Value("${wordmeaning.rate-limit.window-seconds:60}") long windowSeconds) {
        this.capacity = capacity;
        this.windowMs = windowSeconds * 1000L;
    }

    // 한도를 넘었으면 RateLimitExceededException 을 던진다. 넘지 않았으면 사용량을 1 올리고 통과시킨다.
    public void check(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("rate limit 대상 사용자가 없습니다.");
        }

        long now = System.currentTimeMillis();
        sweepIfDue(now);

        // compute 는 같은 키에 대해 원자적으로 실행되므로 연타(동시 요청)에도 카운트가 새지 않는다.
        Window updated = windows.compute(userId, (key, current) -> {
            if (current == null || now - current.startMs() >= windowMs) {
                return new Window(now, 1);
            }
            return new Window(current.startMs(), current.count() + 1);
        });

        if (updated.count() > capacity) {
            long retryAfterSeconds = Math.max(1, (windowMs - (now - updated.startMs()) + 999) / 1000);
            log.warn("호출 제한 초과: userId={}, {}회/{}초", userId, updated.count(), windowMs / 1000);
            throw new RateLimitExceededException(
                    "요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요.", retryAfterSeconds);
        }
    }

    private void sweepIfDue(long now) {
        long lastSweep = lastSweepMs.get();
        // 윈도 10개 주기마다 한 번. 만료된 윈도만 지우므로 진행 중인 카운트는 건드리지 않는다.
        if (now - lastSweep < windowMs * 10) {
            return;
        }
        if (!lastSweepMs.compareAndSet(lastSweep, now)) {
            return;
        }
        windows.values().removeIf(window -> now - window.startMs() >= windowMs);
    }

    private record Window(long startMs, int count) {
    }
}
