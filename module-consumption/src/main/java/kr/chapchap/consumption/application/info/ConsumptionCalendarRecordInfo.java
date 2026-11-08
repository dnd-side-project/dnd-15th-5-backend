package kr.chapchap.consumption.application.info;

import kr.chapchap.consumption.domain.entity.Consumption;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionCalendarRecordInfo(
        Long consumptionId,
        Long placeId,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        String category,
        Long amount
) {

    public static ConsumptionCalendarRecordInfo from(Consumption consumption) {
        return new ConsumptionCalendarRecordInfo(
                consumption.getId(),
                consumption.getPlaceId(),
                consumption.getPurchaseDate(),
                consumption.getPurchaseTime(),
                consumption.getCategory(),
                consumption.getAmount()
        );
    }
}
