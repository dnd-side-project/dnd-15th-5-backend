package kr.chapchap.report.application.info;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record CurrentStatusInfo(
        LocalDate date,
        List<Integer> weeklyCounts,
        int monthlyCount,
        Map<String, Integer> monthlyCategoryCounts,
        String recentDiscoveryMessage,
        List<AcquiredSticker> monthlyStickers,
        YearMonth firstAvailableYearMonth
) {
}
