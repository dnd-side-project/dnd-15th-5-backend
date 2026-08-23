package kr.chapchap.place.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chapchap.place.google-places")
public record GooglePlacesProperties(
        @NotNull URI baseUri,
        @NotBlank String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
