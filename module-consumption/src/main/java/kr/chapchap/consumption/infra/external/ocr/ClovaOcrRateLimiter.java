package kr.chapchap.consumption.infra.external.ocr;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.consumption.infra.config.ClovaOcrProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class ClovaOcrRateLimiter {

    private static final String RATE_LIMIT_KEY = "chapchap:consumption:clova-ocr:next-allowed-at";
    private static final long LIMIT_EXCEEDED = -1L;

    private static final RedisScript<Long> RESERVE_REQUEST_SLOT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                            local intervalMillis = tonumber(ARGV[1])
                            local maxWaitMillis = tonumber(ARGV[2])
                            local redisTime = redis.call('TIME')
                            local nowMillis = tonumber(redisTime[1]) * 1000
                                    + math.floor(tonumber(redisTime[2]) / 1000)
                            local nextAllowedAt = tonumber(redis.call('GET', KEYS[1]))

                            if nextAllowedAt == nil or nextAllowedAt < nowMillis then
                                nextAllowedAt = nowMillis
                            end

                            local waitMillis = nextAllowedAt - nowMillis
                            if waitMillis > maxWaitMillis then
                                return -1
                            end

                            local newNextAllowedAt = nextAllowedAt + intervalMillis
                            redis.call(
                                'PSETEX',
                                KEYS[1],
                                maxWaitMillis + intervalMillis,
                                tostring(newNextAllowedAt)
                            )
                            return waitMillis
                            """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final long requestIntervalMillis;
    private final long maxWaitMillis;

    public ClovaOcrRateLimiter(
            StringRedisTemplate redisTemplate,
            ClovaOcrProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.requestIntervalMillis = requirePositiveMillis(
                properties.requestInterval(),
                "CLOVA OCR 요청 간격"
        );
        this.maxWaitMillis = requirePositiveMillis(
                properties.maxWait(),
                "CLOVA OCR 최대 대기 시간"
        );
    }

    public void awaitPermit() {
        Long waitMillis = reserveRequestSlot();
        if (waitMillis == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        if (waitMillis == LIMIT_EXCEEDED) {
            throw new BusinessException(
                    ConsumptionErrorCode.RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED
            );
        }
        if (waitMillis < 0 || waitMillis > maxWaitMillis) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        waitForRequestSlot(waitMillis);
    }

    private Long reserveRequestSlot() {
        try {
            return redisTemplate.execute(
                    RESERVE_REQUEST_SLOT_SCRIPT,
                    List.of(RATE_LIMIT_KEY),
                    Long.toString(requestIntervalMillis),
                    Long.toString(maxWaitMillis)
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    exception
            );
        }
    }

    private void waitForRequestSlot(long waitMillis) {
        if (waitMillis == 0) {
            return;
        }
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    exception
            );
        }
    }

    private long requirePositiveMillis(Duration duration, String propertyName) {
        long millis = duration.toMillis();
        if (millis <= 0) {
            throw new IllegalArgumentException(propertyName + "은 1ms 이상이어야 합니다.");
        }
        return millis;
    }
}
