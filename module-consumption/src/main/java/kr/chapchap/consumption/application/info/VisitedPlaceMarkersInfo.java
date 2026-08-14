package kr.chapchap.consumption.application.info;

import java.util.List;

public record VisitedPlaceMarkersInfo(List<VisitedPlaceMarkerInfo> markers, int month, int monthlyCount) {
}
