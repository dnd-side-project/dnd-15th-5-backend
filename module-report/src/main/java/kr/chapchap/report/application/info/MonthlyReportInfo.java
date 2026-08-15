package kr.chapchap.report.application.info;

import kr.chapchap.report.domain.entity.TimeSlot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// 월간 리포트 조회 결과
public record MonthlyReportInfo(
        Long reportId,
        YearMonth yearMonth,
        PersonaInfo persona,
        List<PlaceRankInfo> placeRanks,
        List<TownRankInfo> townRanks,
        SummaryInfo summary,
        List<CategoryStatInfo> categoryStats,
        TimePatternInfo timePattern
) {

    public record PersonaInfo(String type, String typeName, List<String> keywords, ScoresInfo scores) {
    }

    public record ScoresInfo(
            BigDecimal scoreExploration,
            BigDecimal scoreTownExpansion,
            BigDecimal scoreDaytime,
            BigDecimal scoreImpulsive
    ) {
    }


    public record PlaceRankInfo(int rank, String placeName, int visitCount, LocalDate firstVisitedDate, String category,
                                 List<String> stickerNames) {
    }

    public record TownRankInfo(int rank, String townName, int visitCount) {
    }

    public record SummaryInfo(int totalVisitCount, int newTownCount, int newPlaceCount) {
    }

    public record CategoryStatInfo(String category, BigDecimal percentage) {
    }

    public record TimePatternInfo(int peakDayOfWeek, TimeSlot peakTimeSlot, List<DayOfWeekCountInfo> dayOfWeekPattern) {
    }

    public record DayOfWeekCountInfo(int dayOfWeek, int visitCount) {
    }
}
