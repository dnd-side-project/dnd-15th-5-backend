package kr.chapchap.report.application.info;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReportCalendarRecord(
        Long consumptionId,
        Long placeId,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        String category,
        Long amount
) {
}
