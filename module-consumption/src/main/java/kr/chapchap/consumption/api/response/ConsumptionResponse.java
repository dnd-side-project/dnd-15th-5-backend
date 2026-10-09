package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ConsumptionInfo;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionResponse(
        Long id,
        Long placeId,
        String placeName,
        String category,
        Long amount,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        @Schema(
                description = "Google Places 대표 사진의 단기 URL(장소당 최대 1장). 사진이 없거나 조회에 실패하면 null",
                nullable = true
        )
        String thumbnailUrl
) {

    public static ConsumptionResponse from(ConsumptionInfo info) {
        return new ConsumptionResponse(
                info.id(),
                info.placeId(),
                info.placeName(),
                info.category(),
                info.amount(),
                info.purchaseDate(),
                info.purchaseTime(),
                info.thumbnailUrl()
        );
    }
}
