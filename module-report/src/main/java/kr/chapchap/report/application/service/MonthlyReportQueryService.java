package kr.chapchap.report.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.CategoryStatInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.DayOfWeekCountInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.DiscoveryInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.PersonaInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.PlaceRankInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.ScoresInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.SummaryInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.TimePatternInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo.TownRankInfo;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.domain.entity.Report;
import kr.chapchap.report.domain.entity.ReportCategoryStat;
import kr.chapchap.report.domain.entity.ReportPlaceRank;
import kr.chapchap.report.domain.entity.ReportTimePattern;
import kr.chapchap.report.domain.entity.ReportTownRank;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MonthlyReportQueryService {

    private static final int NO_VISIT_HOUR = 0;
    private static final int NO_DAY_OF_WEEK = 0;
    private static final int TOP_PLACE_RANK = 1;

    private final ReportRepository reportRepository;
    private final ReportCategoryStatRepository reportCategoryStatRepository;
    private final ReportTownRankRepository reportTownRankRepository;
    private final ReportPlaceRankRepository reportPlaceRankRepository;
    private final ReportTimePatternRepository reportTimePatternRepository;
    private final ConsumptionActivityPort consumptionActivityPort;

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
                toDiscoveryInfo(report),
                toSummaryInfo(report),
                toCategoryStatInfos(categoryStats),
                toTimePatternInfo(timePatterns)
        );
    }

    private PersonaInfo toPersonaInfo(Report report) {
        return new PersonaInfo(
                report.getPersonaType().name(),
                report.getPersonaType().getTypeName(),
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
                .map(placeRank -> new PlaceRankInfo(
                        placeRank.getRank(),
                        placeRank.getPlaceName(),
                        placeRank.getVisitCount(),
                        placeRank.getFirstVisitedDate(),
                        placeRank.getRank() == TOP_PLACE_RANK ? findCategory(command, placeRank.getPlaceId()) : null
                ))
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

    private List<TownRankInfo> toTownRankInfos(List<ReportTownRank> townRanks) {
        return townRanks.stream()
                .map(townRank -> new TownRankInfo(townRank.getRank(), townRank.getTownName(), townRank.getVisitCount()))
                .toList();
    }

    private DiscoveryInfo toDiscoveryInfo(Report report) {
        int newStickerCount = report.getNewStickerCount();
        String message = "지난달 대비 새로운 스티커가 " + newStickerCount + "개가 추가되었어요.";
        return new DiscoveryInfo(message, newStickerCount);
    }

    private SummaryInfo toSummaryInfo(Report report) {
        return new SummaryInfo(report.getTotalVisitCount(), report.getNewTownCount(), report.getNewPlaceCount());
    }

    private List<CategoryStatInfo> toCategoryStatInfos(List<ReportCategoryStat> categoryStats) {
        return categoryStats.stream()
                .map(categoryStat -> new CategoryStatInfo(categoryStat.getCategory(), categoryStat.getPercentage()))
                .toList();
    }

    private TimePatternInfo toTimePatternInfo(List<ReportTimePattern> timePatterns) {
        List<DayOfWeekCountInfo> dayOfWeekPattern = aggregateByDayOfWeek(timePatterns);

        ReportTimePattern peak = timePatterns.stream()
                .max(Comparator.comparingInt(ReportTimePattern::getVisitCount)
                        .thenComparing(Comparator.comparingInt(ReportTimePattern::getDayOfWeek).reversed())
                        .thenComparing(Comparator.comparingInt(ReportTimePattern::getVisitHour).reversed()))
                .orElse(null);

        int peakDayOfWeek = peak != null ? peak.getDayOfWeek() : NO_DAY_OF_WEEK;
        int peakHour = peak != null ? peak.getVisitHour() : NO_VISIT_HOUR;

        return new TimePatternInfo(peakDayOfWeek, peakHour, dayOfWeekPattern);
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
