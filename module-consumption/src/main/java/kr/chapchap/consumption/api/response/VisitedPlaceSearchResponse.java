package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo.VisitedPlaceInfo;

import java.util.List;

public record VisitedPlaceSearchResponse(
        List<VisitedPlaceItem> places,
        boolean hasNext,
        String nextCursor
) {

    public static VisitedPlaceSearchResponse from(VisitedPlaceSearchInfo info) {
        return new VisitedPlaceSearchResponse(
                info.places().stream()
                        .map(VisitedPlaceItem::from)
                        .toList(),
                info.hasNext(),
                info.nextCursor()
        );
    }

    public record VisitedPlaceItem(
            Long placeId,
            String placeName,
            String roadAddress,
            String thumbnailUrl,
            String googleMapsUri
    ) {

        private static VisitedPlaceItem from(VisitedPlaceInfo info) {
            return new VisitedPlaceItem(
                    info.placeId(),
                    info.placeName(),
                    info.roadAddress(),
                    info.thumbnailUrl(),
                    info.googleMapsUri()
            );
        }
    }
}
