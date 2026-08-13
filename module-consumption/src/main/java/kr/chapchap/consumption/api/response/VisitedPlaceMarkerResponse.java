package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;

import java.util.List;

public record VisitedPlaceMarkerResponse(List<VisitedPlaceMarkerItem> places) {

    public static VisitedPlaceMarkerResponse from(List<VisitedPlaceMarkerInfo> infos) {
        return new VisitedPlaceMarkerResponse(infos.stream().map(VisitedPlaceMarkerItem::from).toList());
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
