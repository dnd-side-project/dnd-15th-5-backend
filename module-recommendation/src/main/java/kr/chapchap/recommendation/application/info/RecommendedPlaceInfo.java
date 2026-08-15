package kr.chapchap.recommendation.application.info;

public record RecommendedPlaceInfo(
        Long placeId,
        String name,
        String dongName,
        String category,
        Double latitude,
        Double longitude,
        Long visitCount,
        boolean liked
) {
}
