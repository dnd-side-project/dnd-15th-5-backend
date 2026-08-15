package kr.chapchap.recommendation.application.port;

import kr.chapchap.recommendation.application.info.PlacePopularityInfo;

import java.util.List;

public interface PopularityLookupPort {

    List<PlacePopularityInfo> aggregateByPlaceIds(List<Long> placeIds);
}
