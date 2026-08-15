package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionSearchCommand;
import kr.chapchap.consumption.application.info.ConsumptionActivityInfo;
import kr.chapchap.consumption.application.info.ConsumptionInfo;
import kr.chapchap.consumption.application.info.ConsumptionScrollInfo;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ConsumptionQueryService {

    private static final String UNKNOWN_PLACE_NAME = "알 수 없는 가게";
    private static final PlaceSummaryInfo UNKNOWN_PLACE_SUMMARY =
            new PlaceSummaryInfo(UNKNOWN_PLACE_NAME, null, null, null, null);

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final PlaceSummaryLookupPort placeSummaryLookupPort;

    public ConsumptionScrollInfo search(ConsumptionSearchCommand command) {
        YearMonth yearMonth = command.yearMonth();
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEndExclusive = yearMonth.plusMonths(1).atDay(1);

        int fetchSize = command.size() + 1;
        List<Consumption> fetched = consumptionQueryRepository.searchByCursor(
                command.userId(),
                monthStart,
                monthEndExclusive,
                command.cursorPurchaseDate(),
                command.cursorPurchaseTime(),
                command.cursorId(),
                fetchSize
        );

        boolean hasNext = fetched.size() > command.size();
        List<Consumption> content = hasNext ? fetched.subList(0, command.size()) : fetched;

        List<Long> placeIds = content.stream().map(Consumption::getPlaceId).distinct().toList();
        Map<Long, PlaceSummaryInfo> summaries = placeSummaryLookupPort.findSummaries(placeIds);

        List<ConsumptionInfo> consumptions = content.stream()
                .map(consumption -> ConsumptionInfo.of(
                        consumption,
                        summaries.getOrDefault(consumption.getPlaceId(), UNKNOWN_PLACE_SUMMARY).name()
                ))
                .toList();

        Consumption last = content.isEmpty() ? null : content.get(content.size() - 1);

        return new ConsumptionScrollInfo(
                consumptions,
                hasNext,
                last != null ? last.getPurchaseDate() : null,
                last != null ? last.getPurchaseTime() : null,
                last != null ? last.getId() : null
        );
    }

    public List<ConsumptionActivityInfo> getActivities(Long userId, LocalDate from, LocalDate toExclusive) {
        return consumptionQueryRepository.findAllByUserAndDateRange(userId, from, toExclusive).stream()
                .map(ConsumptionActivityInfo::from)
                .toList();
    }

    public List<Long> getActiveUserIds(LocalDate from, LocalDate toExclusive) {
        return consumptionQueryRepository.findDistinctUserIdsByDateRange(from, toExclusive);
    }
}
