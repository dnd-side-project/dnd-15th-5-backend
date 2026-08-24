package kr.chapchap.recommendation.application.port;

import java.util.Set;

public interface VisitedPlaceLookupPort {

    Set<Long> findVisitedPlaceIds(Long userId);
}
