package kr.chapchap.consumption.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chapchap.consumption.receipt-ocr.clova")
public record ClovaOcrProperties(
        @NotNull URI invokeUrl,
        @NotBlank String secretKey,
        @NotBlank String apiGatewayKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull Duration requestInterval,
        @NotNull Duration maxWait
) {
}
