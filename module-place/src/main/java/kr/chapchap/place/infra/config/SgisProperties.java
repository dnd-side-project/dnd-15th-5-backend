package kr.chapchap.place.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chapchap.place.sgis")
public record SgisProperties(
        @NotNull URI authenticationUri,
        @NotNull URI geocodingUri,
        @NotNull URI reverseGeocodingUri,
        @NotBlank String consumerKey,
        @NotBlank String consumerSecret,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
