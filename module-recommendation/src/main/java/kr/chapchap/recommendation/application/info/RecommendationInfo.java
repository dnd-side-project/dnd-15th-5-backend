package kr.chapchap.recommendation.application.info;

import java.util.List;

public record RecommendationInfo(
        String currentDongName,
        List<RecommendedPlaceInfo> allPlaces,
        List<CategoryPlacesInfo> categoryPlaces
) {

    public record CategoryPlacesInfo(String category, List<RecommendedPlaceInfo> places) {
    }
}
