package kr.chapchap.consumption.domain.entity;

import java.time.LocalDate;

public record PlaceVisitStatsRow(String category, Long totalVisitCount, LocalDate firstVisitedDate) {
}
