package kr.chapchap.consumption.application.port;

import java.util.Set;


public interface PlaceLikeLookupPort {

    Set<Long> findLikedPlaceIds(Long userId);
}
