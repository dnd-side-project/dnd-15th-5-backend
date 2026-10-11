package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ConsumptionDetailInfo;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "소비기록 한 건 조회 결과")
public record ConsumptionDetailResponse(
        @Schema(description = "소비기록 ID")
        Long consumptionId,

        @Schema(description = "장소 ID")
        Long placeId,

        @Schema(description = "Google Place ID")
        String googlePlaceId,

        @Schema(description = "장소명")
        String placeName,

        @Schema(description = "도로명주소")
        String roadAddress,

        @Schema(description = "위도")
        Double latitude,

        @Schema(description = "경도")
        Double longitude,

        @Schema(description = "Google Places 장소 썸네일 URL, 없거나 조회 실패 시 null", nullable = true)
        String thumbnailUrl,

        @Schema(description = "소비 날짜")
        LocalDate purchaseDate,

        @Schema(description = "소비 시간")
        LocalTime purchaseTime,

        @Schema(description = "소비 금액, 미입력 시 null", nullable = true)
        Long amount,

        @Schema(description = "소비 카테고리")
        String category,

        @Schema(description = "선택한 스티커 ID, 사진 기록이면 null", nullable = true)
        Long stickerItemId,

        @Schema(description = "스티커 카테고리, 사진 기록이면 null", nullable = true)
        String stickerCategory,

        @Schema(description = "스티커 이름, 사진 기록이면 null", nullable = true)
        String stickerName,

        @Schema(description = "업로드한 사진 ID, 스티커 기록이면 null", nullable = true)
        Long imageId,

        @Schema(description = "업로드한 사진의 2분간 유효한 조회 URL, 스티커 기록이면 null", nullable = true)
        String imageUrl,

        @Schema(description = "메모, 미입력 시 null", nullable = true)
        String memo
) {
    public static ConsumptionDetailResponse from(ConsumptionDetailInfo info) {
        return new ConsumptionDetailResponse(
                info.consumptionId(),
                info.placeId(),
                info.googlePlaceId(),
                info.placeName(),
                info.roadAddress(),
                info.latitude(),
                info.longitude(),
                info.thumbnailUrl(),
                info.purchaseDate(),
                info.purchaseTime(),
                info.amount(),
                info.category(),
                info.stickerItemId(),
                info.stickerCategory(),
                info.stickerName(),
                info.imageId(),
                info.imageUrl(),
                info.memo()
        );
    }
}
