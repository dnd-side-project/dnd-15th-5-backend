package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.info.ConsumptionActivityInfo;
import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ConsumptionActivityAdapter implements ConsumptionActivityPort {

    private final ConsumptionQueryService consumptionQueryService;

    @Override
    public List<ConsumptionActivity> findActivities(Long userId, LocalDate from, LocalDate toExclusive) {
        return consumptionQueryService.getActivities(userId, from, toExclusive).stream()
                .map(this::toConsumptionActivity)
                .toList();
    }

    @Override
    public List<Long> findActiveUserIds(LocalDate from, LocalDate toExclusive) {
        return consumptionQueryService.getActiveUserIds(from, toExclusive);
    }

    @Override
    public Optional<YearMonth> findFirstAvailableYearMonth(Long userId) {
        return consumptionQueryService.getFirstAvailableYearMonth(userId);
    }

    private ConsumptionActivity toConsumptionActivity(ConsumptionActivityInfo info) {
        return new ConsumptionActivity(info.placeId(), info.category(), info.purchaseDate(), info.purchaseTime());
    }
}
