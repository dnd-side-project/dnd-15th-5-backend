package kr.chapchap.report.infra.external;

import kr.chapchap.place.application.service.PlaceQueryService;
import kr.chapchap.report.application.port.DongNameLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class DongNameLookupAdapter implements DongNameLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Map<Long, String> findDongNames(List<Long> placeIds) {
        return placeQueryService.findDongNamesByIds(placeIds);
    }
}
