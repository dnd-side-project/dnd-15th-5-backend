package kr.chapchap.consumption.domain.entity;

import java.time.LocalDate;

public record PlacePopularityRow(Long placeId, String category, Long visitCount, LocalDate lastVisitedDate) {
}
