package kr.chapchap.report.application.info;

import java.time.YearMonth;
import java.util.List;

public record CurrentStatusInfo(
        YearMonth yearMonth,
        List<Integer> weeklyCounts,
        int monthlyCount,
        String recentDiscoveryMessage
) {
}
