package kr.chapchap.report.application.info;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record CurrentStatusInfo(
        YearMonth yearMonth,
        List<Integer> weeklyCounts,
        int monthlyCount,
        Map<String, Integer> monthlyCategoryCounts,
        String recentDiscoveryMessage
) {
}
