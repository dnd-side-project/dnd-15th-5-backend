package kr.chapchap.consumption.api.response;

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
        LocalTime purchaseTime
) {

    public static ConsumptionResponse from(ConsumptionInfo info) {
        return new ConsumptionResponse(
                info.id(),
                info.placeId(),
                info.placeName(),
                info.category(),
                info.amount(),
                info.purchaseDate(),
                info.purchaseTime()
        );
    }
}
