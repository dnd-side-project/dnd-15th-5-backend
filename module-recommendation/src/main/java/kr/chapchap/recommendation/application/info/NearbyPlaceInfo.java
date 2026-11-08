package kr.chapchap.recommendation.application.info;

public record NearbyPlaceInfo(
        Long placeId,
        String name,
        String dongName,
        String roadAddress,
        Double latitude,
        Double longitude,
        String googlePlaceId
) {
}
