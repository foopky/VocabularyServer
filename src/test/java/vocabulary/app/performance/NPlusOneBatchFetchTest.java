package vocabulary.app.performance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * 같은 측정을 hibernate.default_batch_fetch_size=100 설정으로 다시 한다.
 *
 * <p>batch fetch는 연관 엔티티를 1건씩이 아니라 IN 절로 100건씩 묶어 가져오므로
 * 쿼리 수를 N → N/100 수준으로 줄여준다. 다만 데이터가 100건을 넘으면 다시 늘어나므로
 * "데이터 양과 무관하게 일정"하지는 않다. 그래서 이 클래스는 측정만 하고 회귀 검사는 하지 않는다.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_batch_fetch_size=100")
class NPlusOneBatchFetchTest extends NPlusOneQueryCountTest {

    @Override
    protected String label() {
        return "batch-fetch-size-100";
    }

    @Override
    @Test
    @Disabled("batch fetch는 쿼리를 줄일 뿐 데이터 양과 무관해지지는 않으므로 측정만 한다")
    void queryCountDoesNotGrowWithData() {
    }
}
