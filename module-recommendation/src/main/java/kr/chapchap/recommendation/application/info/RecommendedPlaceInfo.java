package kr.chapchap.recommendation.application.info;

public record RecommendedPlaceInfo(
        Long placeId,
        String name,
        String dongName,
        String category,
        Double latitude,
        Double longitude,
        Long visitCount,
        boolean liked,
        String thumbnailUrl,
        String googleMapsUri,
        String googlePlaceId
) {

    public RecommendedPlaceInfo withPhoto(String thumbnailUrl, String googleMapsUri) {
        return new RecommendedPlaceInfo(
                placeId, name, dongName, category, latitude, longitude, visitCount, liked,
                thumbnailUrl, googleMapsUri, googlePlaceId);
    }
}
