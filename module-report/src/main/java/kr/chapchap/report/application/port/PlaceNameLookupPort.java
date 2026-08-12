package kr.chapchap.report.application.port;

import java.util.List;
import java.util.Map;

public interface PlaceNameLookupPort {

    Map<Long, String> findPlaceNames(List<Long> placeIds);
}
