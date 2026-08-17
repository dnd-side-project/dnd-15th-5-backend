package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ConsumptionInfo;

@Schema(description = "소비 기록 등록 결과")
public record ConsumptionCreateResponse(
        @Schema(description = "생성된 소비 기록 ID", example = "31")
        Long consumptionId
) {

    public static ConsumptionCreateResponse from(ConsumptionInfo info) {
        return new ConsumptionCreateResponse(info.id());
    }
}
