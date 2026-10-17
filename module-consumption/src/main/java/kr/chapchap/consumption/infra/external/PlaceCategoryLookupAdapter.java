package kr.chapchap.consumption.infra.external;

import kr.chapchap.consumption.application.info.PlacePopularityInfo;
import kr.chapchap.consumption.application.service.PopularityQueryService;
import kr.chapchap.place.application.port.PlaceCategoryLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class PlaceCategoryLookupAdapter implements PlaceCategoryLookupPort {

    private final PopularityQueryService popularityQueryService;

    @Override
    public Map<Long, String> findCategoriesByPlaceIds(List<Long> placeIds) {
        return popularityQueryService.aggregatePopularityByPlaceIds(placeIds).stream()
                .collect(Collectors.toMap(PlacePopularityInfo::placeId, PlacePopularityInfo::category));
    }
}
