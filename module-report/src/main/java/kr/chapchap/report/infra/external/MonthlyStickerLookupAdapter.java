package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.service.ConsumptionQueryService;
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
    public List<String> findRecentStickerNames(Long userId, LocalDate from, LocalDate toExclusive) {
        return consumptionQueryService.findRecentStickerNames(userId, from, toExclusive);
    }
}
