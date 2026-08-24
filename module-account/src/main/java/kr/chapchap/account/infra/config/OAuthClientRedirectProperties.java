package kr.chapchap.account.infra.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "chapchap.oauth.client")
public record OAuthClientRedirectProperties(
        @NotNull URI webRedirectUri,
        @NotNull URI appRedirectUri
) {
}
