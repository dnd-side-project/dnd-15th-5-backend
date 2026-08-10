package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.MonthlyReportInfo;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record MonthlyReportResponse(
        Long reportId,
        String yearMonth,
        PersonaResponse persona,
        List<PlaceRankResponse> placeRanks,
        List<TownRankResponse> townRanks,
        DiscoveryResponse discovery,
        SummaryResponse summary,
        List<CategoryStatResponse> categoryStats,
        TimePatternResponse timePattern
) {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public static MonthlyReportResponse from(MonthlyReportInfo info) {
        return new MonthlyReportResponse(
                info.reportId(),
                info.yearMonth().format(YEAR_MONTH_FORMAT),
                PersonaResponse.from(info.persona()),
                info.placeRanks().stream().map(PlaceRankResponse::from).toList(),
                info.townRanks().stream().map(TownRankResponse::from).toList(),
                DiscoveryResponse.from(info.discovery()),
                SummaryResponse.from(info.summary()),
                info.categoryStats().stream().map(CategoryStatResponse::from).toList(),
                TimePatternResponse.from(info.timePattern())
        );
    }
}
