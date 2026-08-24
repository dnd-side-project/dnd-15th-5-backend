package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.info.AcquiredStickerInfo;
import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.report.application.info.AcquiredSticker;
import kr.chapchap.report.application.port.MonthlyStickerLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MonthlyStickerLookupAdapter implements MonthlyStickerLookupPort {

    private final ConsumptionQueryService consumptionQueryService;

    @Override
    public List<AcquiredSticker> findRecentAcquiredStickers(Long userId, LocalDate from, LocalDate toExclusive) {
        return consumptionQueryService.findRecentAcquiredStickers(userId, from, toExclusive).stream()
                .map(this::toAcquiredSticker)
                .toList();
    }

    private AcquiredSticker toAcquiredSticker(AcquiredStickerInfo info) {
        return new AcquiredSticker(info.itemName(), info.acquiredDate());
    }
}
