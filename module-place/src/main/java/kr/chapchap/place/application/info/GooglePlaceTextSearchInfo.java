package kr.chapchap.place.application.info;

public record GooglePlaceTextSearchInfo(
        String googlePlaceId,
        String placeName,
        String roadAddress,
        double latitude,
        double longitude,
        String photoName
) {
}
