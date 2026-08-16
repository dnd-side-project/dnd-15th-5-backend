package kr.chapchap.recommendation.infra.external;

import kr.chapchap.consumption.application.service.PopularityQueryService;
import kr.chapchap.recommendation.application.info.PlacePopularityInfo;
import kr.chapchap.recommendation.application.port.PopularityLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RecommendationPopularityLookupAdapter implements PopularityLookupPort {

    private final PopularityQueryService popularityQueryService;

    @Override
    public List<PlacePopularityInfo> aggregateByPlaceIds(List<Long> placeIds) {
        return popularityQueryService.aggregatePopularityByPlaceIds(placeIds).stream()
                .map(info -> new PlacePopularityInfo(info.placeId(), info.category(), info.visitCount(), info.lastVisitedDate()))
                .toList();
    }
}
