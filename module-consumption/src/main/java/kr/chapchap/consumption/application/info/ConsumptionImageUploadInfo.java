package kr.chapchap.consumption.application.info;

import java.time.LocalDateTime;

public record ConsumptionImageUploadInfo(Long imageId, LocalDateTime expiresAt) {
}
