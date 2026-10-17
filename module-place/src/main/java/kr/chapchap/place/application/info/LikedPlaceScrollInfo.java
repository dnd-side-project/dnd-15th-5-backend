package kr.chapchap.place.application.info;

import java.util.List;

public record LikedPlaceScrollInfo(
        List<LikedPlaceInfo> places,
        List<String> categories,
        boolean hasNext,
        Long nextCursorId
) {

    public record LikedPlaceInfo(
            Long placeId,
            String name,
            String category,
            String address,
            Double latitude,
            Double longitude,
            List<PlacePhotoInfo> photos
    ) {
    }
}
