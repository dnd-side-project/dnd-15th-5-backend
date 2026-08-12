package kr.chapchap.report.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;

public record VisitActivity(
        Long placeId,
        String dongName,
        String category,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {
}
