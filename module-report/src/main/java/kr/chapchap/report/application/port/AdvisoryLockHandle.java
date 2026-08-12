package kr.chapchap.report.application.port;


public interface AdvisoryLockHandle extends AutoCloseable {
    @Override
    void close();
}
