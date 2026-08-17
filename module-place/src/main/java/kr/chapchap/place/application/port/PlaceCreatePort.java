package kr.chapchap.place.application.port;

import kr.chapchap.place.domain.entity.Place;

public interface PlaceCreatePort {

    Long createOrGet(Place place);
}
