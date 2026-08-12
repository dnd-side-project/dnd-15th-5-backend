package kr.chapchap.report.infra.persistence;

import kr.chapchap.report.application.port.AdvisoryLockHandle;
import kr.chapchap.report.application.port.AdvisoryLockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;


@RequiredArgsConstructor
@Component
public class PostgresAdvisoryLockAdapter implements AdvisoryLockPort {

    private final DataSource dataSource;

    @Override
    public Optional<AdvisoryLockHandle> tryLock(long key) {
        Connection connection = openConnection();
        try {
            if (!tryAcquire(connection, key)) {
                closeQuietly(connection);
                return Optional.empty();
            }
            return Optional.of(new SessionAdvisoryLockHandle(connection, key));
        } catch (SQLException exception) {
            closeQuietly(connection);
            throw new IllegalStateException("월간 리포트 배치 락 획득에 실패했습니다.", exception);
        }
    }

    private Connection openConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            throw new IllegalStateException("월간 리포트 배치 락 전용 커넥션을 얻지 못했습니다.", exception);
        }
    }

    private boolean tryAcquire(Connection connection, long key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            statement.setLong(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    // 이 커넥션은 락을 잡은 순간부터 close()로 풀릴 때까지, 커넥션 풀이 아닌 이 핸들이 단독 소유한다.
    private record SessionAdvisoryLockHandle(Connection connection, long key) implements AdvisoryLockHandle {

        @Override
        public void close() {
            try (connection) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                    statement.setLong(1, key);
                    statement.execute();
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("월간 리포트 배치 락 해제에 실패했습니다.", exception);
            }
        }
    }
}
