package kr.chapchap.report.infra.external;

import kr.chapchap.consumption.application.info.ConsumptionCalendarRecordInfo;
import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.report.application.info.ReportCalendarRecord;
import kr.chapchap.report.application.port.ConsumptionCalendarLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ConsumptionCalendarLookupAdapter implements ConsumptionCalendarLookupPort {

    private final ConsumptionQueryService consumptionQueryService;

    @Override
    public List<ReportCalendarRecord> findByDateRange(Long userId, LocalDate from, LocalDate toExclusive) {
        return consumptionQueryService.getActivitiesForCalendar(userId, from, toExclusive).stream()
                .map(this::toReportCalendarRecord)
                .toList();
    }

    @Override
    public Map<Long, Integer> findVisitOrdinals(Long userId, List<Long> placeIds, LocalDate toExclusive) {
        return consumptionQueryService.getVisitOrdinals(userId, placeIds, toExclusive);
    }

    private ReportCalendarRecord toReportCalendarRecord(ConsumptionCalendarRecordInfo info) {
        return new ReportCalendarRecord(
                info.consumptionId(), info.placeId(), info.purchaseDate(), info.purchaseTime(),
                info.category(), info.amount());
    }
}
