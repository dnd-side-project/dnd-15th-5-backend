package kr.chapchap.account.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chapchap.oauth.google")
public record GoogleOAuthProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotNull URI redirectUri,
        @NotNull URI authorizationUri,
        @NotNull URI tokenUri,
        @NotNull URI revokeUri,
        @NotNull URI jwkSetUri,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
