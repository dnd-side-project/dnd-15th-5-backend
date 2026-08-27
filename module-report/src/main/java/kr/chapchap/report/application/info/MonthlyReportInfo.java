package kr.chapchap.report.application.info;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record MonthlyReportInfo(
        Long reportId,
        YearMonth yearMonth,
        PersonaInfo persona,
        List<PlaceRankInfo> placeRanks,
        List<TownRankInfo> townRanks,
        SummaryInfo summary,
        List<CategoryStatInfo> categoryStats,
        TimePatternInfo timePattern,
        YearMonth firstAvailableYearMonth,
        AdjacentPersonaInfo previous,
        AdjacentPersonaInfo next
) {

    public record PersonaInfo(String type, String typeName, String description, List<String> keywords, ScoresInfo scores) {
    }

    public record AdjacentPersonaInfo(YearMonth yearMonth, String type) {
    }

    public record ScoresInfo(
            BigDecimal scoreExploration,
            BigDecimal scoreTownExpansion,
            BigDecimal scoreDaytime,
            BigDecimal scoreImpulsive
    ) {
    }


    public record PlaceRankInfo(int rank, Long placeId, String placeName, int visitCount, LocalDate firstVisitedDate,
                                 String category, List<String> stickerNames) {
    }

    public record TownRankInfo(int rank, String townName, int visitCount) {
    }

    public record SummaryInfo(int totalVisitCount, int newTownCount, int newPlaceCount) {
    }

    public record CategoryStatInfo(String category, BigDecimal percentage) {
    }

    public record TimePatternInfo(int peakDayOfWeek, String peakTimeSlot, List<DayOfWeekCountInfo> dayOfWeekPattern) {
    }

    public record DayOfWeekCountInfo(int dayOfWeek, int visitCount) {
    }
}
