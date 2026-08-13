package kr.chapchap.consumption.application.port;

import kr.chapchap.consumption.application.info.PlaceLocationInfo;

import java.util.List;
import java.util.Map;


public interface PlaceLocationLookupPort {

    Map<Long, PlaceLocationInfo> findLocations(List<Long> placeIds);
}
