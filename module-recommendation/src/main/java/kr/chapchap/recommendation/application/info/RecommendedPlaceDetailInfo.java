package kr.chapchap.recommendation.application.info;

import java.util.List;

public record RecommendedPlaceDetailInfo(
        Long placeId,
        String name,
        String category,
        String dongName,
        Double latitude,
        Double longitude,
        List<String> thumbnailUrls,
        String googleMapsUri
) {
}
