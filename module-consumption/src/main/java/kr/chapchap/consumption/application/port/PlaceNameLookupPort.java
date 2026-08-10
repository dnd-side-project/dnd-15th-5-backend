package kr.chapchap.consumption.application.port;

import java.util.List;
import java.util.Map;


public interface PlaceNameLookupPort {

    Map<Long, String> findNames(List<Long> placeIds);
}
