package kr.chapchap.recommendation.application.info;

import java.util.List;

public record RecommendedPlaceDetailInfo(
        Long placeId,
        String googlePlaceId,
        String name,
        String category,
        String dongName,
        String roadAddress,
        Double latitude,
        Double longitude,
        List<String> thumbnailUrls,
        String googleMapsUri
) {
}
