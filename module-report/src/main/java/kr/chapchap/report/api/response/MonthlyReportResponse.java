package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.MonthlyReportInfo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;


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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    public record PersonaResponse(String type, String typeName, ScoresResponse scores) {

        public static PersonaResponse from(MonthlyReportInfo.PersonaInfo info) {
            return new PersonaResponse(info.type(), info.typeName(), ScoresResponse.from(info.scores()));
        }
    }

    public record ScoresResponse(
            BigDecimal scoreExploration,
            BigDecimal scoreTownExpansion,
            BigDecimal scoreDaytime,
            BigDecimal scoreImpulsive
    ) {

        public static ScoresResponse from(MonthlyReportInfo.ScoresInfo info) {
            return new ScoresResponse(
                    info.scoreExploration(),
                    info.scoreTownExpansion(),
                    info.scoreDaytime(),
                    info.scoreImpulsive()
            );
        }
    }

    public record PlaceRankResponse(int rank, String placeName, int visitCount, String firstVisitedDate, String category) {

        public static PlaceRankResponse from(MonthlyReportInfo.PlaceRankInfo info) {
            String firstVisitedDate = info.firstVisitedDate() != null ? DATE_FORMAT.format(info.firstVisitedDate()) : null;
            return new PlaceRankResponse(info.rank(), info.placeName(), info.visitCount(), firstVisitedDate, info.category());
        }
    }

    public record TownRankResponse(int rank, String townName, int visitCount) {

        public static TownRankResponse from(MonthlyReportInfo.TownRankInfo info) {
            return new TownRankResponse(info.rank(), info.townName(), info.visitCount());
        }
    }

    public record DiscoveryResponse(String message, int newStickerCount) {

        public static DiscoveryResponse from(MonthlyReportInfo.DiscoveryInfo info) {
            return new DiscoveryResponse(info.message(), info.newStickerCount());
        }
    }

    public record SummaryResponse(int totalVisitCount, int newTownCount, int newPlaceCount) {

        public static SummaryResponse from(MonthlyReportInfo.SummaryInfo info) {
            return new SummaryResponse(info.totalVisitCount(), info.newTownCount(), info.newPlaceCount());
        }
    }

    public record CategoryStatResponse(String category, BigDecimal percentage) {

        public static CategoryStatResponse from(MonthlyReportInfo.CategoryStatInfo info) {
            return new CategoryStatResponse(info.category(), info.percentage());
        }
    }

    public record TimePatternResponse(String peakDayOfWeek, int peakHour, List<DayOfWeekCountResponse> dayOfWeekPattern) {

        public static TimePatternResponse from(MonthlyReportInfo.TimePatternInfo info) {
            return new TimePatternResponse(
                    toDayOfWeekCode(info.peakDayOfWeek()),
                    info.peakHour(),
                    info.dayOfWeekPattern().stream().map(DayOfWeekCountResponse::from).toList()
            );
        }


        private static String toDayOfWeekCode(int dayOfWeek) {
            if (dayOfWeek == 0) {
                return null;
            }
            return DayOfWeek.of(dayOfWeek).name().substring(0, 3).toUpperCase(Locale.ROOT);
        }
    }

    public record DayOfWeekCountResponse(int dayOfWeek, int visitCount) {

        public static DayOfWeekCountResponse from(MonthlyReportInfo.DayOfWeekCountInfo info) {
            return new DayOfWeekCountResponse(info.dayOfWeek(), info.visitCount());
        }
    }
}
