package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;

import java.util.List;

public record VisitedPlaceMarkerResponse(List<VisitedPlaceMarkerItem> places, int monthlyCount) {

    public static VisitedPlaceMarkerResponse from(VisitedPlaceMarkersInfo info) {
        List<VisitedPlaceMarkerItem> items = info.markers().stream().map(VisitedPlaceMarkerItem::from).toList();
        return new VisitedPlaceMarkerResponse(items, info.monthlyCount());
    }

    public record VisitedPlaceMarkerItem(
            Long placeId,
            String placeName,
            String category,
            Double latitude,
            Double longitude,
            long visitCount
    ) {
        public static VisitedPlaceMarkerItem from(VisitedPlaceMarkerInfo info) {
            return new VisitedPlaceMarkerItem(
                    info.placeId(), info.placeName(), info.category(),
                    info.latitude(), info.longitude(), info.visitCount()
            );
        }
    }
}
