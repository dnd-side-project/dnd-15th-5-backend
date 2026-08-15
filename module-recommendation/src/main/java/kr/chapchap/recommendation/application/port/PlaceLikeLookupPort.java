package kr.chapchap.recommendation.application.port;

import java.util.Set;

public interface PlaceLikeLookupPort {

    Set<Long> findLikedPlaceIds(Long userId);
}
