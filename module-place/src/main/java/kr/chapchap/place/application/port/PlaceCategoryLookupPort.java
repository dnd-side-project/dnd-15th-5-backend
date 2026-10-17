package kr.chapchap.place.application.port;

import java.util.List;
import java.util.Map;

public interface PlaceCategoryLookupPort {

    // 전체 소비기록 수가 가장 많은 카테고리. 기록이 없는 장소는 결과에서 제외한다.
    Map<Long, String> findCategoriesByPlaceIds(List<Long> placeIds);
}
