package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소비기록 이미지 조회 URL 재발급 결과")
public record ConsumptionImageUrlResponse(
        @Schema(description = "요청한 이미지 ID (요청값과 동일)")
        Long imageId,

        @Schema(description = "이 응답 시점부터 10분간 유효한 조회 URL")
        String imageUrl
) {
}
