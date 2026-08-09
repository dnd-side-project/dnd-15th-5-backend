package kr.chapchap.consumption.infra.external;

import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.consumption.application.port.PlaceNameLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Component
public class PlaceNameLookupAdapter implements PlaceNameLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Map<Long, String> findNames(List<Long> placeIds) {
        return placeQueryService.findNamesByIds(placeIds);
    }
}
