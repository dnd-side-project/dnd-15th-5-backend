package kr.chapchap.account.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chapchap.account.profile-image")
public record ProfileImageStorageProperties(
        @NotBlank String bucket,
        @NotNull Duration readUrlExpiration
) {

    private static final Duration MAX_READ_URL_EXPIRATION = Duration.ofDays(7);

    public ProfileImageStorageProperties {
        if (readUrlExpiration != null) {
            if (readUrlExpiration.isZero() || readUrlExpiration.isNegative()) {
                throw new IllegalArgumentException("프로필 이미지 조회 URL 만료 시간은 0보다 커야 합니다.");
            }
            if (readUrlExpiration.compareTo(MAX_READ_URL_EXPIRATION) > 0) {
                throw new IllegalArgumentException("프로필 이미지 조회 URL 만료 시간은 7일 이하여야 합니다.");
            }
        }
    }
}
