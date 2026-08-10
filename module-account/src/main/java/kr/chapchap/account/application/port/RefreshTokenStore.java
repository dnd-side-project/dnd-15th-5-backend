package kr.chapchap.account.application.port;

import java.time.Duration;

public interface RefreshTokenStore {

    void save(Long userId, String tokenId, Duration ttl);

    boolean consume(Long userId, String tokenId);
}
