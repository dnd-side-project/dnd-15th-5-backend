package kr.chapchap.receipt.api.response;

import kr.chapchap.receipt.application.info.ConsumptionInfo;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionResponse(
        Long id,
        Long placeId,
        String category,
        Long amount,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {

    public static ConsumptionResponse from(ConsumptionInfo info) {
        return new ConsumptionResponse(
                info.id(),
                info.placeId(),
                info.category(),
                info.amount(),
                info.purchaseDate(),
                info.purchaseTime()
        );
    }
}
