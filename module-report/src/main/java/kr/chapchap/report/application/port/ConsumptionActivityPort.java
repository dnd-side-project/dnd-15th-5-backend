package kr.chapchap.report.application.port;

import kr.chapchap.report.application.info.ConsumptionActivity;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ConsumptionActivityPort {
    List<ConsumptionActivity> findActivities(Long userId, LocalDate from, LocalDate toExclusive);

    List<Long> findActiveUserIds(LocalDate from, LocalDate toExclusive);

    Optional<YearMonth> findFirstAvailableYearMonth(Long userId);
}
