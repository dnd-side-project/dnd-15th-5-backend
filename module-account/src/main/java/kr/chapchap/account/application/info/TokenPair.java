package kr.chapchap.account.application.info;

import java.time.Duration;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String refreshTokenId,
        Duration refreshTokenExpiresIn
) {
}
