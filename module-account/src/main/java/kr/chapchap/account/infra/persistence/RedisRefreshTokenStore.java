package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.application.port.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Repository
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "chapchap:account:refresh-token:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long userId, String tokenId, Duration ttl) {
        redisTemplate.opsForValue().set(
                createKey(userId, tokenId),
                "active",
                Objects.requireNonNull(ttl, "리프레시 토큰 TTL은 필수입니다.")
        );
    }

    @Override
    public boolean consume(Long userId, String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.delete(createKey(userId, tokenId)));
    }

    @Override
    public void revokeAll(Long userId) {
        List<String> keys = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(createUserKeyPrefix(userId) + "*")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            cursor.forEachRemaining(keys::add);
        }

        if (!keys.isEmpty()) {
            redisTemplate.unlink(keys);
        }
    }

    private String createKey(Long userId, String tokenId) {
        return createUserKeyPrefix(userId)
                + Objects.requireNonNull(tokenId, "리프레시 토큰 ID는 필수입니다.");
    }

    private String createUserKeyPrefix(Long userId) {
        return KEY_PREFIX
                + Objects.requireNonNull(userId, "사용자 ID는 필수입니다.")
                + ":";
    }
}
