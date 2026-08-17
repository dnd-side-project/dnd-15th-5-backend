package kr.chapchap.consumption.infra.external.ocr;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.consumption.infra.config.ClovaOcrProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ClovaOcrRateLimiterTest {

    private static final String RATE_LIMIT_KEY =
            "chapchap:consumption:clova-ocr:next-allowed-at";

    private StringRedisTemplate redisTemplate;
    private ClovaOcrRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        rateLimiter = new ClovaOcrRateLimiter(
                redisTemplate,
                properties(Duration.ofMillis(1100), Duration.ofSeconds(5))
        );
    }

    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    @Test
    void 대기할_필요가_없는_OCR_요청은_즉시_허용한다() {
        // given
        givenReserveResult(0L);

        // when & then
        assertThatCode(rateLimiter::awaitPermit).doesNotThrowAnyException();
    }

    @Test
    void OCR_호출_대기_시간이_한도를_넘으면_요청_제한_예외를_던진다() {
        // given
        givenReserveResult(-1L);

        // when & then
        assertThatThrownBy(rateLimiter::awaitPermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ConsumptionErrorCode.RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED
                        )
                );
    }

    @Test
    void Redis가_OCR_호출_대기_시간을_반환하지_않으면_외부_서비스_예외를_던진다() {
        // given
        givenReserveResult(null);

        // when & then
        assertThatThrownBy(rateLimiter::awaitPermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
    }

    @Test
    void Redis_연동에_실패하면_원인_예외를_보존하고_외부_서비스_예외를_던진다() {
        // given
        RedisConnectionFailureException cause =
                new RedisConnectionFailureException("redis unavailable");
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(RATE_LIMIT_KEY)),
                eq("1100"),
                eq("5000")
        )).willThrow(cause);

        // when & then
        assertThatThrownBy(rateLimiter::awaitPermit)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(
                            CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                    );
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

    @Test
    void OCR_호출을_기다리다_중단되면_인터럽트_상태를_유지하고_호출을_중단한다() {
        // given
        givenReserveResult(1L);
        Thread.currentThread().interrupt();

        // when & then
        assertThatThrownBy(rateLimiter::awaitPermit)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(
                            CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                    );
                    assertThat(exception.getCause()).isInstanceOf(InterruptedException.class);
                });
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void 호출_간격과_최대_대기_시간은_1ms_이상이어야_한다() {
        // when & then
        assertThatThrownBy(() -> new ClovaOcrRateLimiter(
                redisTemplate,
                properties(Duration.ZERO, Duration.ofSeconds(5))
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClovaOcrRateLimiter(
                redisTemplate,
                properties(Duration.ofMillis(1100), Duration.ZERO)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private void givenReserveResult(Long result) {
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(RATE_LIMIT_KEY)),
                eq("1100"),
                eq("5000")
        )).willReturn(result);
    }

    private ClovaOcrProperties properties(
            Duration requestInterval,
            Duration maxWait
    ) {
        return new ClovaOcrProperties(
                URI.create("https://ocr.example.com/custom/general"),
                "clova-secret-key",
                "api-gateway-key",
                Duration.ofSeconds(3),
                Duration.ofSeconds(15),
                requestInterval,
                maxWait
        );
    }
}
