package kr.chapchap.account;

import kr.chapchap.core.test.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.target=23")
@DirtiesContext
class NotificationSettingsMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    @Test
    void 기존_전체_설정을_두_설정에_보존하고_신규_회원은_기본_ON으로_생성한다() {
        // given
        jdbcTemplate.update("INSERT INTO users (id, nickname, status, push_enabled) VALUES (1, '켜짐', 'ACTIVE', true)");
        jdbcTemplate.update("INSERT INTO users (id, nickname, status, push_enabled) VALUES (2, '꺼짐', 'ACTIVE', false)");

        // when
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("24").load().migrate();
        jdbcTemplate.update("INSERT INTO users (id, nickname, status) VALUES (3, '신규', 'ACTIVE')");

        // then
        for (long id : new long[]{1L, 2L, 3L}) {
            Map<String, Object> settings = jdbcTemplate.queryForMap(
                    "SELECT consumption_reminder_enabled, monthly_report_enabled FROM users WHERE id = ?", id
            );
            assertThat(settings.get("consumption_reminder_enabled")).isEqualTo(id != 2L);
            assertThat(settings.get("monthly_report_enabled")).isEqualTo(id != 2L);
        }
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'push_enabled'
                """, Long.class)).isZero();
    }
}
