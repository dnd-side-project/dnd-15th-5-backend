package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ConsumptionImageUploadInfo;
import java.time.LocalDateTime;

public record ConsumptionImageUploadResponse(
        @Schema(description = "소비기록 등록 시 전달할 이미지 ID") Long imageId,
        @Schema(description = "임시 이미지 만료 시각. 업로드 후 24시간, 소비기록에 연결하면 만료되지 않음")
        LocalDateTime expiresAt
) {
    public static ConsumptionImageUploadResponse from(ConsumptionImageUploadInfo info) {
        return new ConsumptionImageUploadResponse(info.imageId(), info.expiresAt());
    }
}
