package kr.chapchap.consumption.application.info;

import kr.chapchap.consumption.domain.entity.Consumption;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionActivityInfo(
        Long placeId,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {

    public static ConsumptionActivityInfo from(Consumption consumption) {
        return new ConsumptionActivityInfo(
                consumption.getPlaceId(),
                consumption.getPurchaseDate(),
                consumption.getPurchaseTime()
        );
    }
}
