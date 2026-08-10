package kr.chapchap.account.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chapchap.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration signupExpiration,
        @NotNull Duration accessExpiration,
        @NotNull Duration refreshExpiration
) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret != null
                && secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT 비밀키는 32바이트 이상이어야 합니다.");
        }

        validateExpiration("가입 토큰", signupExpiration);
        validateExpiration("접근 토큰", accessExpiration);
        validateExpiration("리프레시 토큰", refreshExpiration);
    }

    private static void validateExpiration(String tokenName, Duration expiration) {
        if (expiration != null && (expiration.isZero() || expiration.isNegative())) {
            throw new IllegalArgumentException(tokenName + " 만료 시간은 0보다 커야 합니다.");
        }
    }
}
