package kr.chapchap.receipt.application.info;

import kr.chapchap.receipt.domain.entity.Consumption;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionInfo(
        Long id,
        Long placeId,
        String placeName,
        String category,
        Long amount,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {

    public static ConsumptionInfo of(Consumption consumption, String placeName) {
        return new ConsumptionInfo(
                consumption.getId(),
                consumption.getPlaceId(),
                placeName,
                consumption.getCategory(),
                consumption.getAmount(),
                consumption.getPurchaseDate(),
                consumption.getPurchaseTime()
        );
    }
}
