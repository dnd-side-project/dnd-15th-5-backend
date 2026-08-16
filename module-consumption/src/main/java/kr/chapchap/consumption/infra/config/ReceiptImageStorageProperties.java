package kr.chapchap.consumption.infra.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chapchap.consumption.receipt-image")
public record ReceiptImageStorageProperties(@NotBlank String bucket) {
}
