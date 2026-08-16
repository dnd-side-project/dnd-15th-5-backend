package kr.chapchap.place.application.info;

import kr.chapchap.place.domain.entity.Place;

public record PlaceSummaryInfo(String name, String dongName, String address, Double latitude, Double longitude) {

    public static PlaceSummaryInfo from(Place place) {
        return new PlaceSummaryInfo(
                place.getName(), place.getAdministrativeDongName(), place.getRoadAddress(),
                place.getLatitude(), place.getLongitude()
        );
    }
}
