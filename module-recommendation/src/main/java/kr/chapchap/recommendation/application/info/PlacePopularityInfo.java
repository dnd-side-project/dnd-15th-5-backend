package kr.chapchap.recommendation.application.info;

import java.time.LocalDate;

public record PlacePopularityInfo(Long placeId, String category, Long visitCount, LocalDate lastVisitedDate) {
}
