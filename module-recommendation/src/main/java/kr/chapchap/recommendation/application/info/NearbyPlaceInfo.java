package kr.chapchap.recommendation.application.info;

public record NearbyPlaceInfo(
        Long placeId,
        String name,
        String dongName,
        Double latitude,
        Double longitude,
        String googlePlaceId
) {
}
