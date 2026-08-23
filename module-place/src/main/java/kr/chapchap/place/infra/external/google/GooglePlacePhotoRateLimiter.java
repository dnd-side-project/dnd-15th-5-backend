package kr.chapchap.place.infra.external.google;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.exception.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GooglePlacePhotoRateLimiter {

    private static final String RATE_LIMIT_KEY_PREFIX =
            "chapchap:place:google-places:photo-media:";
    private static final int MONTHLY_LIMIT = 1_500;
    private static final long LIMIT_EXCEEDED = -1L;
    private static final ZoneId RATE_LIMIT_ZONE = ZoneId.of("Asia/Seoul");

    private static final RedisScript<Long> ACQUIRE_PERMIT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                            local limit = tonumber(ARGV[1])
                            local current = tonumber(redis.call('GET', KEYS[1]) or '0')

                            if current >= limit then
                                return -1
                            end

                            local count = redis.call('INCR', KEYS[1])
                            redis.call('EXPIREAT', KEYS[1], tonumber(ARGV[2]))
                            return count
                            """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public void acquirePermit() {
        YearMonth currentMonth = YearMonth.now(clock.withZone(RATE_LIMIT_ZONE));
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + currentMonth;
        long expiresAt = currentMonth.plusMonths(1)
                .atDay(1)
                .atStartOfDay(RATE_LIMIT_ZONE)
                .toEpochSecond();

        Long count;
        try {
            count = redisTemplate.execute(
                    ACQUIRE_PERMIT_SCRIPT,
                    List.of(rateLimitKey),
                    Integer.toString(MONTHLY_LIMIT),
                    Long.toString(expiresAt)
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    exception
            );
        }

        if (count == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        if (count == LIMIT_EXCEEDED) {
            throw new BusinessException(PlaceErrorCode.PHOTO_REQUEST_LIMIT_EXCEEDED);
        }
        if (count < 1 || count > MONTHLY_LIMIT) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }
}
