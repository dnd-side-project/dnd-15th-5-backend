package kr.chapchap.report.application.port;


public interface AdvisoryLockPort {
    // 현재 트랜잭션 범위에서 락 획득을 시도한다. 트랜잭션이 끝나면(commit/rollback) 자동으로 해제된다.
    boolean tryLock(long key);
}
