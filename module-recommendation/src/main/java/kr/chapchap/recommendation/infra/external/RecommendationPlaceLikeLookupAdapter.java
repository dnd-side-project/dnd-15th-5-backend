package kr.chapchap.recommendation.infra.external;

import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.recommendation.application.port.PlaceLikeLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@RequiredArgsConstructor
@Component
public class RecommendationPlaceLikeLookupAdapter implements PlaceLikeLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Set<Long> findLikedPlaceIds(Long userId) {
        return placeQueryService.findLikedPlaceIds(userId);
    }
}
