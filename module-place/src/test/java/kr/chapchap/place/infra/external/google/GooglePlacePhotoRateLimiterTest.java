package kr.chapchap.place.infra.external.google;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GooglePlacePhotoRateLimiterTest {

    private static final String RATE_LIMIT_KEY =
            "chapchap:place:google-places:photo-media:2026-08";
    private static final String EXPIRES_AT = Long.toString(
            Instant.parse("2026-08-31T15:00:00Z").getEpochSecond()
    );

    private StringRedisTemplate redisTemplate;
    private GooglePlacePhotoRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        rateLimiter = new GooglePlacePhotoRateLimiter(
                redisTemplate,
                Clock.fixed(Instant.parse("2026-08-23T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void Photo_Media를_호출할_때_월간_한도_이내이면_요청을_허용한다() {
        // given
        givenAcquireResult(1_500L);

        // when & then
        assertThatCode(rateLimiter::acquirePermit).doesNotThrowAnyException();
    }

    @Test
    void Photo_Media를_호출할_때_월간_한도를_초과하면_예외를_던진다() {
        // given
        givenAcquireResult(-1L);

        // when & then
        assertThatThrownBy(rateLimiter::acquirePermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                PlaceErrorCode.PHOTO_REQUEST_LIMIT_EXCEEDED
                        )
                );
    }

    @Test
    void Photo_Media를_호출할_때_Redis가_결과를_반환하지_않으면_외부_서비스_예외를_던진다() {
        // given
        givenAcquireResult(null);

        // when & then
        assertThatThrownBy(rateLimiter::acquirePermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
    }

    @Test
    void Photo_Media를_호출할_때_Redis_연동에_실패하면_원인을_보존한다() {
        // given
        RedisConnectionFailureException cause =
                new RedisConnectionFailureException("redis unavailable");
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(RATE_LIMIT_KEY)),
                eq("1500"),
                eq(EXPIRES_AT)
        )).willThrow(cause);

        // when & then
        assertThatThrownBy(rateLimiter::acquirePermit)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(
                            CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                    );
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

    private void givenAcquireResult(Long result) {
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(RATE_LIMIT_KEY)),
                eq("1500"),
                eq(EXPIRES_AT)
        )).willReturn(result);
    }
}
