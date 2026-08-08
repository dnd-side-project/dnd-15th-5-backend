package kr.chapchap.receipt.application.info;

import kr.chapchap.receipt.domain.entity.Consumption;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionInfo(
        Long id,
        Long placeId,
        String category,
        Long amount,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {

    public static ConsumptionInfo from(Consumption consumption) {
        return new ConsumptionInfo(
                consumption.getId(),
                consumption.getPlaceId(),
                consumption.getCategory(),
                consumption.getAmount(),
                consumption.getPurchaseDate(),
                consumption.getPurchaseTime()
        );
    }
}
