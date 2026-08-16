package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.report.application.port.PlaceStickerLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class PlaceStickerLookupAdapter implements PlaceStickerLookupPort {

    private final ConsumptionQueryService consumptionQueryService;

    @Override
    public List<String> findRecentStickerNames(Long userId, Long placeId, LocalDate from, LocalDate toExclusive, int limit) {
        return consumptionQueryService.findRecentStickerNamesByPlace(userId, placeId, from, toExclusive, limit);
    }
}
