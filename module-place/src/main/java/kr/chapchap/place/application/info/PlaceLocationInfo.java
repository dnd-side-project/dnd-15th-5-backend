package kr.chapchap.place.application.info;

import kr.chapchap.place.domain.entity.Place;

public record PlaceLocationInfo(Long placeId, Double latitude, Double longitude) {

    public static PlaceLocationInfo from(Place place) {
        return new PlaceLocationInfo(place.getId(), place.getLatitude(), place.getLongitude());
    }
}
