package kr.chapchap.consumption.infra.external;

import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.consumption.application.port.PlaceDongNameLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Component
public class PlaceDongNameLookupAdapter implements PlaceDongNameLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Map<Long, String> findDongNames(List<Long> placeIds) {
        return placeQueryService.findDongNamesByIds(placeIds);
    }
}
