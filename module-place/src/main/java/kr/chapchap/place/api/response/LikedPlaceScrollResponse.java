package kr.chapchap.place.api.response;

import kr.chapchap.place.application.info.LikedPlaceScrollInfo;
import kr.chapchap.place.application.info.LikedPlaceScrollInfo.LikedPlaceInfo;

import java.util.List;

public record LikedPlaceScrollResponse(
        List<LikedPlaceResponse> places,
        List<String> categories,
        boolean hasNext,
        Long nextCursorId
) {

    public static LikedPlaceScrollResponse from(LikedPlaceScrollInfo info) {
        return new LikedPlaceScrollResponse(
                info.places().stream().map(LikedPlaceResponse::from).toList(),
                info.categories(), info.hasNext(), info.nextCursorId()
        );
    }

    public record LikedPlaceResponse(
            Long placeId,
            String name,
            String category,
            String address,
            Double latitude,
            Double longitude,
            List<PhotoResponse> photos
    ) {

        public static LikedPlaceResponse from(LikedPlaceInfo info) {
            return new LikedPlaceResponse(
                    info.placeId(), info.name(), info.category(), info.address(), info.latitude(), info.longitude(),
                    info.photos().stream()
                            .map(photo -> new PhotoResponse(photo.thumbnailUrl(), photo.googleMapsUri()))
                            .toList()
            );
        }
    }

    public record PhotoResponse(String thumbnailUrl, String googleMapsUri) {
    }
}
