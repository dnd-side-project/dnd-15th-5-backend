package kr.chapchap.recommendation.infra.external;

import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.recommendation.application.info.NearbyPlaceInfo;
import kr.chapchap.recommendation.application.port.PlaceRadiusLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RecommendationPlaceRadiusLookupAdapter implements PlaceRadiusLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public List<NearbyPlaceInfo> findWithinRadius(double latitude, double longitude, double radiusMeters) {
        return placeQueryService.findWithinRadius(latitude, longitude, radiusMeters).stream()
                .map(info -> new NearbyPlaceInfo(
                        info.placeId(), info.name(), info.dongName(), info.latitude(), info.longitude()))
                .toList();
    }
}
