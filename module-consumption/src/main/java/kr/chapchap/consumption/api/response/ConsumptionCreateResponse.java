package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;

@Schema(description = "소비 기록 등록 결과")
public record ConsumptionCreateResponse(
        @Schema(description = "생성된 소비 기록 ID", example = "31")
        Long consumptionId,

        @Schema(description = "획득한 스티커 카테고리", example = "공통")
        String stickerCategory,

        @Schema(description = "획득한 스티커 이름", example = "눈")
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
