package kr.chapchap.consumption.application.port;

import java.util.List;
import java.util.Set;


public interface PlaceLikeLookupPort {

    Set<Long> findLikedPlaceIds(Long userId, List<Long> placeIds);
}
