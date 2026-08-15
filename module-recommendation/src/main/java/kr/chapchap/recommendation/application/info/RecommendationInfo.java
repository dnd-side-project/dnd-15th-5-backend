package kr.chapchap.recommendation.application.info;

import java.util.List;

public record RecommendationInfo(
        List<RecommendedPlaceInfo> myTownPlaces,
        List<RecommendedPlaceInfo> sameCategoryPlaces
) {
}
