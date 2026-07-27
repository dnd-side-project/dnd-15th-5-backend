package com.dnd;

import com.dnd.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DndApplicationTest {

    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 1;

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @Autowired
    DndApplicationTest(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Test
    void Testcontainers의_PostgreSQL과_Redis에_애플리케이션이_정상적으로_연결된다() throws SQLException {
        // given
        try (
                Connection postgresqlConnection = dataSource.getConnection();
                RedisConnection redisConnection = redisConnectionFactory.getConnection()
        ) {
            // when
            boolean isPostgresqlConnected =
                    postgresqlConnection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
            String redisResponse = redisConnection.ping();

            // then
            assertThat(isPostgresqlConnected).isTrue();
            assertThat(redisResponse).isEqualTo("PONG");
        }
    }
}
