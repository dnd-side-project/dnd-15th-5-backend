package kr.chapchap.consumption.application.info;

import java.util.List;

public record VisitedPlaceSearchInfo(
        List<VisitedPlaceInfo> places,
        boolean hasNext,
        String nextCursor
) {

    public record VisitedPlaceInfo(
            Long placeId,
            String placeName,
            String roadAddress,
            String thumbnailUrl,
            String googleMapsUri
    ) {
    }
}
