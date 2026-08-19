package kr.chapchap.recommendation.infra.external;

import kr.chapchap.consumption.application.service.PopularityQueryService;
import kr.chapchap.recommendation.application.port.VisitedPlaceLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@RequiredArgsConstructor
@Component
public class RecommendationVisitedPlaceLookupAdapter implements VisitedPlaceLookupPort {

    private final PopularityQueryService popularityQueryService;

    @Override
    public Set<Long> findVisitedPlaceIds(Long userId) {
        return popularityQueryService.findVisitedPlaceIds(userId);
    }
}
