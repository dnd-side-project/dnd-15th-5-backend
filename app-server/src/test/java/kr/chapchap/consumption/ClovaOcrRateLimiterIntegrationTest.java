package kr.chapchap.consumption;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.consumption.infra.config.ClovaOcrProperties;
import kr.chapchap.consumption.infra.external.ocr.ClovaOcrRateLimiter;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ClovaOcrRateLimiterIntegrationTest {

    private static final String RATE_LIMIT_KEY =
            "chapchap:consumption:clova-ocr:next-allowed-at";

    private final StringRedisTemplate redisTemplate;

    @Autowired
    ClovaOcrRateLimiterIntegrationTest(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @BeforeEach
    @AfterEach
    void clearRateLimit() {
        redisTemplate.delete(RATE_LIMIT_KEY);
    }

    @Test
    void 같은_Redis를_사용하는_OCR_요청은_호출_간격을_유지한다() {
        // given
        Duration requestInterval = Duration.ofMillis(200);
        ClovaOcrRateLimiter firstLimiter = createRateLimiter(
                requestInterval,
                Duration.ofSeconds(1)
        );
        ClovaOcrRateLimiter secondLimiter = createRateLimiter(
                requestInterval,
                Duration.ofSeconds(1)
        );
        firstLimiter.awaitPermit();

        // when
        long startedAt = System.nanoTime();
        secondLimiter.awaitPermit();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        // then
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(150));
    }

    @Test
    void 다음_OCR_호출의_대기_시간이_한도를_넘으면_요청을_거부한다() {
        // given
        ClovaOcrRateLimiter firstLimiter = createRateLimiter(
                Duration.ofMillis(200),
                Duration.ofMillis(50)
        );
        ClovaOcrRateLimiter secondLimiter = createRateLimiter(
                Duration.ofMillis(200),
                Duration.ofMillis(50)
        );
        firstLimiter.awaitPermit();

        // when & then
        assertThatThrownBy(secondLimiter::awaitPermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ConsumptionErrorCode.RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED
                        )
                );
    }

    private ClovaOcrRateLimiter createRateLimiter(
            Duration requestInterval,
            Duration maxWait
    ) {
        return new ClovaOcrRateLimiter(
                redisTemplate,
                new ClovaOcrProperties(
                        URI.create("https://ocr.example.com/custom/general"),
                        "clova-secret-key",
                        "api-gateway-key",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(15),
                        requestInterval,
                        maxWait
                )
        );
    }
}
