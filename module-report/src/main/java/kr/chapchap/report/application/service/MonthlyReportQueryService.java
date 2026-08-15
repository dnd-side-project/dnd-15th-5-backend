package kr.chapchap.report.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.CategoryStatInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.DayOfWeekCountInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.PersonaInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.PlaceRankInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.ScoresInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.SummaryInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.TimePatternInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.TownRankInfo;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.PlaceStickerLookupPort;
import kr.chapchap.report.domain.entity.Report;
import kr.chapchap.report.domain.entity.ReportCategoryStat;
import kr.chapchap.report.domain.entity.ReportPlaceRank;
import kr.chapchap.report.domain.entity.ReportTimePattern;
import kr.chapchap.report.domain.entity.ReportTownRank;
import kr.chapchap.report.domain.entity.TimeSlot;
import kr.chapchap.report.domain.repository.ReportCategoryStatRepository;
import kr.chapchap.report.domain.repository.ReportPlaceRankRepository;
import kr.chapchap.report.domain.repository.ReportRepository;
import kr.chapchap.report.domain.repository.ReportTimePatternRepository;
import kr.chapchap.report.domain.repository.ReportTownRankRepository;
import kr.chapchap.report.exception.ReportErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MonthlyReportQueryService {

    private static final int NO_DAY_OF_WEEK = 0;
    private static final int TOP_PLACE_RANK = 1;
    private static final int TOP_PLACE_STICKER_LIMIT = 5;

    private final ReportRepository reportRepository;
    private final ReportCategoryStatRepository reportCategoryStatRepository;
    private final ReportTownRankRepository reportTownRankRepository;
    private final ReportPlaceRankRepository reportPlaceRankRepository;
    private final ReportTimePatternRepository reportTimePatternRepository;
    private final ConsumptionActivityPort consumptionActivityPort;
    private final PlaceStickerLookupPort placeStickerLookupPort;

    public MonthlyReportInfo getMonthlyReport(GetMonthlyReportCommand command) {
        Report report = reportRepository.findByUserIdAndReportMonth(command.userId(), command.yearMonth().atDay(1))
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));

        List<ReportCategoryStat> categoryStats = reportCategoryStatRepository.findByReportId(report.getId());
        List<ReportTownRank> townRanks = reportTownRankRepository.findByReportIdOrderByRankAsc(report.getId());
        List<ReportPlaceRank> placeRanks = reportPlaceRankRepository.findByReportIdOrderByRankAsc(report.getId());
        List<ReportTimePattern> timePatterns = reportTimePatternRepository.findByReportId(report.getId());

        return new MonthlyReportInfo(
                report.getId(),
                command.yearMonth(),
                toPersonaInfo(report),
                toPlaceRankInfos(placeRanks, command),
                toTownRankInfos(townRanks),
                toSummaryInfo(report),
                toCategoryStatInfos(categoryStats),
                toTimePatternInfo(timePatterns)
        );
    }

    private PersonaInfo toPersonaInfo(Report report) {
        return new PersonaInfo(
                report.getPersonaType().name(),
                report.getPersonaType().getTypeName(),
                report.getPersonaType().getKeywords(),
                new ScoresInfo(
                        report.getScoreExploration(),
                        report.getScoreTownExpansion(),
                        report.getScoreDaytime(),
                        report.getScoreImpulsive()
                )
        );
    }

    private List<PlaceRankInfo> toPlaceRankInfos(List<ReportPlaceRank> placeRanks, GetMonthlyReportCommand command) {
        return placeRanks.stream()
                .map(placeRank -> {
                    boolean isTopPlace = placeRank.getRank() == TOP_PLACE_RANK;
                    return new PlaceRankInfo(
                            placeRank.getRank(),
                            placeRank.getPlaceName(),
                            placeRank.getVisitCount(),
                            placeRank.getFirstVisitedDate(),
                            isTopPlace ? findCategory(command, placeRank.getPlaceId()) : null,
                            isTopPlace ? findStickerNames(command, placeRank.getPlaceId()) : List.of()
                    );
                })
                .toList();
    }


    private String findCategory(GetMonthlyReportCommand command, Long placeId) {
        LocalDate monthStart = command.yearMonth().atDay(1);
        LocalDate monthEndExclusive = command.yearMonth().plusMonths(1).atDay(1);

        return consumptionActivityPort.findActivities(command.userId(), monthStart, monthEndExclusive).stream()
                .filter(activity -> activity.placeId().equals(placeId))
                .map(ConsumptionActivity::category)
                .findFirst()
                .orElse(null);
    }

    private List<String> findStickerNames(GetMonthlyReportCommand command, Long placeId) {
        LocalDate monthStart = command.yearMonth().atDay(1);
        LocalDate monthEndExclusive = command.yearMonth().plusMonths(1).atDay(1);

        return placeStickerLookupPort.findRecentStickerNames(
                command.userId(), placeId, monthStart, monthEndExclusive, TOP_PLACE_STICKER_LIMIT);
    }

    private List<TownRankInfo> toTownRankInfos(List<ReportTownRank> townRanks) {
        return townRanks.stream()
                .map(townRank -> new TownRankInfo(townRank.getRank(), townRank.getTownName(), townRank.getVisitCount()))
                .toList();
    }

    private SummaryInfo toSummaryInfo(Report report) {
        return new SummaryInfo(report.getTotalVisitCount(), report.getNewTownCount(), report.getNewPlaceCount());
    }

    private List<CategoryStatInfo> toCategoryStatInfos(List<ReportCategoryStat> categoryStats) {
        return categoryStats.stream()
                .map(categoryStat -> new CategoryStatInfo(categoryStat.getCategory(), categoryStat.getPercentage()))
                .toList();
    }

    // 정확한 시(hour) 단위 대신 새벽/아침/점심/저녁/밤 시간대로 묶어서 peak을 구한다 (한두 건 차이로 엇갈리는 걸 방지)
    private TimePatternInfo toTimePatternInfo(List<ReportTimePattern> timePatterns) {
        List<DayOfWeekCountInfo> dayOfWeekPattern = aggregateByDayOfWeek(timePatterns);

        Map<DaySlotKey, Integer> countsBySlot = new LinkedHashMap<>();
        for (ReportTimePattern timePattern : timePatterns) {
            DaySlotKey key = new DaySlotKey(timePattern.getDayOfWeek(), TimeSlot.from(timePattern.getVisitHour()));
            countsBySlot.merge(key, timePattern.getVisitCount(), Integer::sum);
        }

        Optional<Map.Entry<DaySlotKey, Integer>> peak = countsBySlot.entrySet().stream()
                .max(Map.Entry.<DaySlotKey, Integer>comparingByValue()
                        .thenComparing(entry -> entry.getKey().dayOfWeek())
                        .thenComparing(entry -> entry.getKey().timeSlot()));

        int peakDayOfWeek = peak.map(entry -> entry.getKey().dayOfWeek()).orElse(NO_DAY_OF_WEEK);
        TimeSlot peakTimeSlot = peak.map(entry -> entry.getKey().timeSlot()).orElse(null);

        return new TimePatternInfo(peakDayOfWeek, peakTimeSlot, dayOfWeekPattern);
    }

    private record DaySlotKey(int dayOfWeek, TimeSlot timeSlot) {
    }

    private List<DayOfWeekCountInfo> aggregateByDayOfWeek(List<ReportTimePattern> timePatterns) {
        int[] countsByDayOfWeek = new int[7];
        for (ReportTimePattern timePattern : timePatterns) {
            countsByDayOfWeek[timePattern.getDayOfWeek() - 1] += timePattern.getVisitCount();
        }

        return IntStream.rangeClosed(1, 7)
                .mapToObj(dayOfWeek -> new DayOfWeekCountInfo(dayOfWeek, countsByDayOfWeek[dayOfWeek - 1]))
                .toList();
    }
}
