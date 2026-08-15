package kr.chapchap.consumption.infra.external;

import kr.chapchap.consumption.application.port.PlaceLikeLookupPort;
import kr.chapchap.place.application.service.PlaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;


@RequiredArgsConstructor
@Component
public class PlaceLikeLookupAdapter implements PlaceLikeLookupPort {

    private final PlaceQueryService placeQueryService;

    @Override
    public Set<Long> findLikedPlaceIds(Long userId) {
        return placeQueryService.findLikedPlaceIds(userId);
    }
}
