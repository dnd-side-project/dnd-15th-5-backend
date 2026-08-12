package kr.chapchap.report;

import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.report.application.port.AdvisoryLockHandle;
import kr.chapchap.report.application.port.AdvisoryLockPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Blue/Green 배포로 두 인스턴스가 동시에 같은 월을 집계하려 할 때, 하나만 락을 획득해야 한다.
// AdvisoryLockPort.tryLock()은 호출할 때마다 전용 커넥션(=별도 Postgres 세션)을 새로 열기 때문에,
// 같은 스레드에서 두 번 호출하는 것만으로도 "서로 다른 두 세션이 동시에 같은 키를 두고 경합하는" 상황을 재현할 수 있다.
// (그래서 이전 버전과 달리 스레드/래치 동기화가 필요 없다 — 그만큼 테스트가 영원히 멈출 위험도 없다.)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PostgresAdvisoryLockConcurrencyTest {

    private static final long TEST_LOCK_KEY = 424242L;

    private final AdvisoryLockPort advisoryLockPort;

    @Autowired
    PostgresAdvisoryLockConcurrencyTest(AdvisoryLockPort advisoryLockPort) {
        this.advisoryLockPort = advisoryLockPort;
    }

    @Test
    void 같은_키로_동시에_락을_시도하면_하나만_성공한다() {
        // given: 첫 번째 세션이 락을 잡은 상태
        Optional<AdvisoryLockHandle> first = advisoryLockPort.tryLock(TEST_LOCK_KEY);
        try {
            assertThat(first).isPresent();

            // when: 다른 세션이 같은 키로 시도
            Optional<AdvisoryLockHandle> second = advisoryLockPort.tryLock(TEST_LOCK_KEY);

            // then: 이미 잡혀 있으니 실패해야 한다
            assertThat(second).isEmpty();
        } finally {
            first.ifPresent(AdvisoryLockHandle::close);
        }
    }

    @Test
    void 락을_해제하면_다른_세션이_다시_잡을_수_있다() {
        // given: 잡았다가 바로 해제
        advisoryLockPort.tryLock(TEST_LOCK_KEY).ifPresent(AdvisoryLockHandle::close);

        // when
        Optional<AdvisoryLockHandle> afterRelease = advisoryLockPort.tryLock(TEST_LOCK_KEY);

        // then
        assertThat(afterRelease).isPresent();
        afterRelease.ifPresent(AdvisoryLockHandle::close);
    }
}
