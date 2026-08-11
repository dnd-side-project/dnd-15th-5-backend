package kr.chapchap.report;

import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.report.application.port.AdvisoryLockPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

// Blue/Green 배포로 두 인스턴스가 동시에 같은 월을 집계하려 할 때, 하나만 락을 획득해야 한다.
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PostgresAdvisoryLockConcurrencyTest {

    private static final long TEST_LOCK_KEY = 424242L;

    private final AdvisoryLockPort advisoryLockPort;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    PostgresAdvisoryLockConcurrencyTest(AdvisoryLockPort advisoryLockPort, PlatformTransactionManager transactionManager) {
        this.advisoryLockPort = advisoryLockPort;
        this.transactionManager = transactionManager;
    }

    @Test
    void 같은_키로_동시에_락을_시도하면_하나만_성공한다() throws InterruptedException {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        AtomicBoolean firstResult = new AtomicBoolean();
        AtomicBoolean secondResult = new AtomicBoolean();

        Thread first = new Thread(() -> transactionTemplate.executeWithoutResult(status -> {
            firstResult.set(advisoryLockPort.tryLock(TEST_LOCK_KEY));
            firstAcquired.countDown();
            await(secondAttempted);
        }));

        Thread second = new Thread(() -> {
            await(firstAcquired);
            transactionTemplate.executeWithoutResult(status -> secondResult.set(advisoryLockPort.tryLock(TEST_LOCK_KEY)));
            secondAttempted.countDown();
        });

        // when
        first.start();
        second.start();
        first.join();
        second.join();

        // then
        assertThat(firstResult.get()).isTrue();
        assertThat(secondResult.get()).isFalse();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
