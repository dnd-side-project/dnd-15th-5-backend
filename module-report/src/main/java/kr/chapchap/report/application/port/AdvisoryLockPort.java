package kr.chapchap.report.application.port;

import java.util.Optional;

// Blue/Green가 동시에 같은 배치를 실행하지 않도록 막는 논블로킹 락.
public interface AdvisoryLockPort {
    // 락은 Spring 트랜잭션과 무관하게 전용 커넥션 위에서 유지되므로, 호출한 쪽의 트랜잭션 유무와 상관없이 동작
    Optional<AdvisoryLockHandle> tryLock(long key);
}
