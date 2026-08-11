package kr.chapchap.report.infra.persistence;

import kr.chapchap.report.application.port.AdvisoryLockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class PostgresAdvisoryLockAdapter implements AdvisoryLockPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean tryLock(long key) {
        Boolean acquired = jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, key);
        return Boolean.TRUE.equals(acquired);
    }
}
