package kr.chapchap.consumption.application.info;

import kr.chapchap.consumption.domain.entity.Consumption;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionActivityInfo(
        Long placeId,
        String category,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {

    public static ConsumptionActivityInfo from(Consumption consumption) {
        return new ConsumptionActivityInfo(
                consumption.getPlaceId(),
                consumption.getCategory(),
                consumption.getPurchaseDate(),
                consumption.getPurchaseTime()
        );
    }
}
