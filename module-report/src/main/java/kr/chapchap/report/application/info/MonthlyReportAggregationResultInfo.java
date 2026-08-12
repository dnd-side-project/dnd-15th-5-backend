package kr.chapchap.report.application.info;

import java.time.YearMonth;
import java.util.List;

public record MonthlyReportAggregationResultInfo(
        YearMonth yearMonth,
        boolean lockAcquired,
        int targetUserCount,
        int succeededCount,
        List<Long> failedUserIds
) {

    public static MonthlyReportAggregationResultInfo skippedDueToLock(YearMonth yearMonth) {
        return new MonthlyReportAggregationResultInfo(yearMonth, false, 0, 0, List.of());
    }
}
