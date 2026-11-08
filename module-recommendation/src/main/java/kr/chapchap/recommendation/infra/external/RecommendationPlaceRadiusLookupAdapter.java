package kr.chapchap.recommendation.infra.external;

import kr.chapchap.place.application.info.PlaceSummaryInfo;
import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.recommendation.application.info.NearbyPlaceInfo;
import kr.chapchap.recommendation.application.port.PlaceRadiusLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RecommendationPlaceRadiusLookupAdapter implements PlaceRadiusLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public List<NearbyPlaceInfo> findWithinRadius(double latitude, double longitude, double radiusMeters) {
        return placeQueryService.findWithinRadius(latitude, longitude, radiusMeters).stream()
                .map(info -> new NearbyPlaceInfo(
                        info.placeId(), info.name(), info.dongName(), null, info.latitude(), info.longitude(),
                        info.googlePlaceId()))
                .toList();
    }

    @Override
    public Optional<NearbyPlaceInfo> findById(Long placeId) {
        Map<Long, PlaceSummaryInfo> summaries = placeQueryService.findSummariesByIds(List.of(placeId));
        PlaceSummaryInfo summary = summaries.get(placeId);
        if (summary == null) {
            return Optional.empty();
        }
        return Optional.of(new NearbyPlaceInfo(
                placeId, summary.name(), summary.dongName(), summary.address(), summary.latitude(), summary.longitude(),
                summary.googlePlaceId()));
    }
}
