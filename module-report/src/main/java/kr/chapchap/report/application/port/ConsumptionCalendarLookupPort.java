package kr.chapchap.report.application.port;

import kr.chapchap.report.application.info.ReportCalendarRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ConsumptionCalendarLookupPort {

    List<ReportCalendarRecord> findByDateRange(Long userId, LocalDate from, LocalDate toExclusive);

    // key: consumptionId, value: 그 가게에 대한 방문 순번
    Map<Long, Integer> findVisitOrdinals(Long userId, List<Long> placeIds, LocalDate toExclusive);
}
