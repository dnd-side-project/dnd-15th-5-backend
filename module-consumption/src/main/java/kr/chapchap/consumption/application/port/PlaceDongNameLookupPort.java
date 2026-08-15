package kr.chapchap.consumption.application.port;

import java.util.List;
import java.util.Map;


public interface PlaceDongNameLookupPort {

    Map<Long, String> findDongNames(List<Long> placeIds);
}
