package kr.chapchap.report.application.info;

import java.time.YearMonth;
import java.util.List;

public record MonthlyReportInfo(
        Long reportId,
        YearMonth yearMonth,
        PersonaInfo persona,
        List<PlaceRankInfo> placeRanks,
        List<TownRankInfo> townRanks,
        DiscoveryInfo discovery,
        SummaryInfo summary,
        List<CategoryStatInfo> categoryStats,
        TimePatternInfo timePattern
) {
}
