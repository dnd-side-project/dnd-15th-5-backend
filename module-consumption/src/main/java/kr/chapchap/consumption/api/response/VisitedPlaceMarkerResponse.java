package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;

import java.util.List;

public record VisitedPlaceMarkerResponse(List<VisitedPlaceMarkerItem> places, int month, int monthlyPlaceCount) {

    public static VisitedPlaceMarkerResponse from(VisitedPlaceMarkersInfo info) {
        List<VisitedPlaceMarkerItem> items = info.markers().stream().map(VisitedPlaceMarkerItem::from).toList();
        return new VisitedPlaceMarkerResponse(items, info.month(), info.monthlyPlaceCount());
    }

    public record VisitedPlaceMarkerItem(
            Long placeId,
            String placeName,
            String category,
            Double latitude,
            Double longitude,
            long visitCount,
            boolean liked,
            String stickerCategory,
            String stickerName,
            String googlePlaceId
    ) {
        public static VisitedPlaceMarkerItem from(VisitedPlaceMarkerInfo info) {
            return new VisitedPlaceMarkerItem(
                    info.placeId(), info.placeName(), info.category(),
                    info.latitude(), info.longitude(), info.visitCount(),
                    info.liked(), info.stickerCategory(), info.stickerName(),
                    info.googlePlaceId()
            );
        }
    }
}
