package kr.chapchap.consumption.infra.external;

import kr.chapchap.consumption.application.info.PlaceLocationInfo;
import kr.chapchap.consumption.application.port.PlaceLocationLookupPort;
import kr.chapchap.place.application.service.PlaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Component
public class PlaceLocationLookupAdapter implements PlaceLocationLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Map<Long, PlaceLocationInfo> findLocations(List<Long> placeIds) {
        return placeQueryService.findLocationsByIds(placeIds).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new PlaceLocationInfo(
                                entry.getKey(), entry.getValue().latitude(), entry.getValue().longitude())
                ));
    }
}
