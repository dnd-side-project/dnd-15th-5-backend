package kr.chapchap.place;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.exception.PlaceErrorCode;
import kr.chapchap.place.infra.external.google.GooglePlaceTextSearchRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GooglePlaceTextSearchRateLimiterIntegrationTest {

    private static final ZoneId RATE_LIMIT_ZONE = ZoneId.of("Asia/Seoul");
    // 만료 시각(다음 달 1일)이 항상 미래여야 하므로 고정 날짜 대신 실제 현재 시각을 기준으로 삼는다.
    private static final Clock FIXED_CLOCK = Clock.system(RATE_LIMIT_ZONE);
    private static final String RATE_LIMIT_KEY =
            "chapchap:place:google-places:text-search:" + YearMonth.now(FIXED_CLOCK);

    private final StringRedisTemplate redisTemplate;

    @Autowired
    GooglePlaceTextSearchRateLimiterIntegrationTest(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @BeforeEach
    @AfterEach
    void clearRateLimit() {
        redisTemplate.delete(RATE_LIMIT_KEY);
    }

    @Test
    void Text_Search_호출을_제한할_때_월간_사용량이_4500건이면_다음_요청을_거부한다() {
        // given
        redisTemplate.opsForValue().set(RATE_LIMIT_KEY, "4499");
        GooglePlaceTextSearchRateLimiter firstLimiter =
                new GooglePlaceTextSearchRateLimiter(redisTemplate, FIXED_CLOCK);
        GooglePlaceTextSearchRateLimiter secondLimiter =
                new GooglePlaceTextSearchRateLimiter(redisTemplate, FIXED_CLOCK);

        // when
        firstLimiter.acquirePermit();

        // then
        assertThat(redisTemplate.opsForValue().get(RATE_LIMIT_KEY)).isEqualTo("4500");
        assertThat(redisTemplate.getExpire(RATE_LIMIT_KEY)).isPositive();
        assertThatThrownBy(secondLimiter::acquirePermit)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                PlaceErrorCode.TEXT_SEARCH_REQUEST_LIMIT_EXCEEDED
                        )
                );
    }
}
