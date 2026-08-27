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
        SummaryResponse summary,
        List<CategoryStatResponse> categoryStats,
        TimePatternResponse timePattern,
        String firstAvailableYearMonth,
        AdjacentPersonaResponse previous,
        AdjacentPersonaResponse next
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
                SummaryResponse.from(info.summary()),
                info.categoryStats().stream().map(CategoryStatResponse::from).toList(),
                TimePatternResponse.from(info.timePattern()),
                info.firstAvailableYearMonth() != null ? info.firstAvailableYearMonth().format(YEAR_MONTH_FORMAT) : null,
                AdjacentPersonaResponse.from(info.previous()),
                AdjacentPersonaResponse.from(info.next())
        );
    }

    public record AdjacentPersonaResponse(String yearMonth, String type) {

        public static AdjacentPersonaResponse from(MonthlyReportInfo.AdjacentPersonaInfo info) {
            if (info == null) {
                return null;
            }
            return new AdjacentPersonaResponse(info.yearMonth().format(YEAR_MONTH_FORMAT), info.type());
        }
    }

    public record PersonaResponse(String type, String typeName, List<String> keywords, ScoresResponse scores) {

        public static PersonaResponse from(MonthlyReportInfo.PersonaInfo info) {
            return new PersonaResponse(info.type(), info.typeName(), info.keywords(), ScoresResponse.from(info.scores()));
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

    public record PlaceRankResponse(int rank, Long placeId, String placeName, int visitCount, String firstVisitedDate,
                                     String category, List<String> stickerNames) {

        public static PlaceRankResponse from(MonthlyReportInfo.PlaceRankInfo info) {
            String firstVisitedDate = info.firstVisitedDate() != null ? DATE_FORMAT.format(info.firstVisitedDate()) : null;
            return new PlaceRankResponse(info.rank(), info.placeId(), info.placeName(), info.visitCount(), firstVisitedDate,
                    info.category(), info.stickerNames());
        }
    }

    public record TownRankResponse(int rank, String townName, int visitCount) {

        public static TownRankResponse from(MonthlyReportInfo.TownRankInfo info) {
            return new TownRankResponse(info.rank(), info.townName(), info.visitCount());
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

    public record TimePatternResponse(String peakDayOfWeek, String peakTimeSlot, List<DayOfWeekCountResponse> dayOfWeekPattern) {

        public static TimePatternResponse from(MonthlyReportInfo.TimePatternInfo info) {
            return new TimePatternResponse(
                    toDayOfWeekCode(info.peakDayOfWeek()),
                    info.peakTimeSlot(),
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
