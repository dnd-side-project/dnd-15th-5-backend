package kr.chapchap.recommendation.infra.external;

import kr.chapchap.place.application.service.AdministrativeDongQueryService;
import kr.chapchap.recommendation.application.port.CurrentDongLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CurrentDongLookupAdapter implements CurrentDongLookupPort {

    private final AdministrativeDongQueryService administrativeDongQueryService;

    @Override
    public String findFullName(double latitude, double longitude) {
        return administrativeDongQueryService.findFullNameByCoordinates(latitude, longitude);
    }
}
