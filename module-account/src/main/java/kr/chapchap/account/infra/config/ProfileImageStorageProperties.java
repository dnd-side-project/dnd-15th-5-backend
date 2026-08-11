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

    public ProfileImageStorageProperties {
        if (readUrlExpiration != null
                && (readUrlExpiration.isZero() || readUrlExpiration.isNegative())) {
            throw new IllegalArgumentException("프로필 이미지 조회 URL 만료 시간은 0보다 커야 합니다.");
        }
    }
}
