package kr.chapchap.report.application.info;

import java.time.LocalDate;
import java.time.LocalTime;

//ConsumptionActivityPort의 조회 결과 DTO
public record ConsumptionActivity(
        Long placeId,
        String category,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {
}
