package kr.chapchap.consumption.application.info;

import java.time.LocalDate;

public record PlacePopularityInfo(Long placeId, String category, Long visitCount, LocalDate lastVisitedDate) {
}
