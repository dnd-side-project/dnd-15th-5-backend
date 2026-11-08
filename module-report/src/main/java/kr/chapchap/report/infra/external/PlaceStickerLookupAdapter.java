package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.info.ConsumptionVisualInfo;
import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.report.application.info.ConsumptionVisualSummary;
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
    public List<ConsumptionVisualSummary> findRecentVisuals(Long userId, Long placeId, LocalDate from, LocalDate toExclusive, Integer limit) {
        return consumptionQueryService.findRecentVisualsByPlace(userId, placeId, from, toExclusive, limit).stream()
                .map(this::toSummary)
                .toList();
    }

    private ConsumptionVisualSummary toSummary(ConsumptionVisualInfo info) {
        return new ConsumptionVisualSummary(info.stickerItemId(), info.stickerName(), info.imageId(), info.imageUrl());
    }
}
