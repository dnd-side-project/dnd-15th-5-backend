package kr.chapchap.report.infra.external;

import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.report.application.port.PlaceNameLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Component
public class ReportPlaceNameLookupAdapter implements PlaceNameLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Map<Long, String> findPlaceNames(List<Long> placeIds) {
        return placeQueryService.findNamesByIds(placeIds);
    }
}
