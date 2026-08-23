package kr.chapchap.place;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.exception.PlaceErrorCode;
import kr.chapchap.place.infra.external.google.GooglePlacePhotoRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GooglePlacePhotoRateLimiterIntegrationTest {

    private static final String RATE_LIMIT_KEY =
            "chapchap:place:google-places:photo-media:2026-08";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-23T00:00:00Z"),
            ZoneOffset.UTC
    );

    private final StringRedisTemplate redisTemplate;

    @Autowired
    GooglePlacePhotoRateLimiterIntegrationTest(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @BeforeEach
    @AfterEach
    void clearRateLimit() {
        redisTemplate.delete(RATE_LIMIT_KEY);
    }

    @Test
    void Photo_Media_호출을_제한할_때_월간_사용량이_1500건이면_다음_요청을_거부한다() {
        // given
        redisTemplate.opsForValue().set(RATE_LIMIT_KEY, "1499");
        GooglePlacePhotoRateLimiter firstLimiter = new GooglePlacePhotoRateLimiter(
                redisTemplate,
                FIXED_CLOCK
        );
        GooglePlacePhotoRateLimiter secondLimiter = new GooglePlacePhotoRateLimiter(
                redisTemplate,
                FIXED_CLOCK
        );

        // when
        firstLimiter.acquirePermit();

        // then
        assertThat(redisTemplate.opsForValue().get(RATE_LIMIT_KEY)).isEqualTo("1500");
        assertThat(redisTemplate.getExpire(RATE_LIMIT_KEY)).isPositive();
        assertThatThrownBy(secondLimiter::acquirePermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                PlaceErrorCode.PHOTO_REQUEST_LIMIT_EXCEEDED
                        )
                );
    }
}
