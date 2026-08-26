package kr.chapchap.place.application.info;

public record GooglePlaceSearchResultInfo(
        String googlePlaceId,
        String placeName,
        String roadAddress,
        double latitude,
        double longitude,
        String thumbnailUrl
) {
}
