package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;

@Schema(description = "소비 기록 등록 결과")
public record ConsumptionCreateResponse(
        @Schema(description = "생성된 소비 기록 ID", example = "31")
        Long consumptionId,

        @Schema(description = "선택한 스티커 카테고리, 사진 등록이면 null", example = "카페", nullable = true)
        String stickerCategory,

        @Schema(description = "선택한 스티커 이름, 사진 등록이면 null", example = "커피", nullable = true)
        String stickerName
) {

    public static ConsumptionCreateResponse from(ConsumptionCreateInfo info) {
        return new ConsumptionCreateResponse(
                info.consumptionId(),
                info.stickerCategory(),
                info.stickerName()
        );
    }
}
