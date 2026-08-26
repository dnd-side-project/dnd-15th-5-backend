package kr.chapchap.report.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.notification.application.event.ReportGeneratedEvent;
import kr.chapchap.report.application.command.AggregateMonthlyReportCommand;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.MonthlyReportAggregationResultInfo;
import kr.chapchap.report.application.port.AdvisoryLockHandle;
import kr.chapchap.report.application.port.AdvisoryLockPort;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.DongNameLookupPort;
import kr.chapchap.report.application.port.PlaceNameLookupPort;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult;
import kr.chapchap.report.domain.entity.MonthlyVisitActivity;
import kr.chapchap.report.domain.entity.PersonaScoreResult;
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
import kr.chapchap.report.domain.service.MonthlyAggregationCalculator;
import kr.chapchap.report.domain.service.PersonaScoringService;
import kr.chapchap.report.exception.ReportErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
public class MonthlyReportAggregationService {

    private static final LocalDate ACTIVITY_HISTORY_START = LocalDate.of(2026, 1, 1);
    private static final String UNKNOWN_PLACE_NAME = "알 수 없는 가게";
    private static final long LOCK_NAMESPACE = "monthly-report-batch".hashCode();

    private final ConsumptionActivityPort consumptionActivityPort;
    private final DongNameLookupPort dongNameLookupPort;
    private final PlaceNameLookupPort placeNameLookupPort;
    private final MonthlyAggregationCalculator monthlyAggregationCalculator;
    private final PersonaScoringService personaScoringService;
    private final ReportRepository reportRepository;
    private final ReportCategoryStatRepository reportCategoryStatRepository;
    private final ReportTownRankRepository reportTownRankRepository;
    private final ReportPlaceRankRepository reportPlaceRankRepository;
    private final ReportTimePatternRepository reportTimePatternRepository;
    private final AdvisoryLockPort advisoryLockPort;
    private final TransactionTemplate perUserTransactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public MonthlyReportAggregationService(
            ConsumptionActivityPort consumptionActivityPort,
            DongNameLookupPort dongNameLookupPort,
            PlaceNameLookupPort placeNameLookupPort,
            MonthlyAggregationCalculator monthlyAggregationCalculator,
            PersonaScoringService personaScoringService,
            ReportRepository reportRepository,
            ReportCategoryStatRepository reportCategoryStatRepository,
            ReportTownRankRepository reportTownRankRepository,
            ReportPlaceRankRepository reportPlaceRankRepository,
            ReportTimePatternRepository reportTimePatternRepository,
            AdvisoryLockPort advisoryLockPort,
            PlatformTransactionManager transactionManager,
            ApplicationEventPublisher eventPublisher
    ) {
        this.consumptionActivityPort = consumptionActivityPort;
        this.dongNameLookupPort = dongNameLookupPort;
        this.placeNameLookupPort = placeNameLookupPort;
        this.monthlyAggregationCalculator = monthlyAggregationCalculator;
        this.personaScoringService = personaScoringService;
        this.reportRepository = reportRepository;
        this.reportCategoryStatRepository = reportCategoryStatRepository;
        this.reportTownRankRepository = reportTownRankRepository;
        this.reportPlaceRankRepository = reportPlaceRankRepository;
        this.reportTimePatternRepository = reportTimePatternRepository;
        this.advisoryLockPort = advisoryLockPort;
        this.eventPublisher =eventPublisher;


        TransactionTemplate perUserTemplate = new TransactionTemplate(transactionManager);
        perUserTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.perUserTransactionTemplate = perUserTemplate;
    }

    public MonthlyReportAggregationResultInfo aggregate(AggregateMonthlyReportCommand command) {
        YearMonth yearMonth = command.yearMonth();
        long lockKey = lockKey(yearMonth);

        Optional<AdvisoryLockHandle> lockHandle = advisoryLockPort.tryLock(lockKey);
        if (lockHandle.isEmpty()) {
            log.info("월간 리포트 이미 실행 중이라 건너뜁니다. yearMonth={}", yearMonth);
            return MonthlyReportAggregationResultInfo.skippedDueToLock(yearMonth);
        }

        try {
            List<Long> targetUserIds = consumptionActivityPort.findActiveUserIds(yearMonth.atDay(1), yearMonth.plusMonths(1).atDay(1));
            List<Long> failedUserIds = new ArrayList<>();
            int succeeded = 0;

            for (Long userId : targetUserIds) {
                try {
                    perUserTransactionTemplate.executeWithoutResult(status -> {
                        aggregateForUser(userId, yearMonth);
                        log.info("이벤트 발행 직전. 트랜잭션 동기화 활성?={}",
                                org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive());
                        eventPublisher.publishEvent(new ReportGeneratedEvent(userId, yearMonth));
                        log.info("이벤트 발행 완료. userId={}", userId);
                    });
                    succeeded++;
                } catch (Exception exception) {
                    BusinessException businessException = new BusinessException(ReportErrorCode.MONTHLY_REPORT_AGGREGATION_FAILED, exception);
                    log.error("[{}] {} userId={}, yearMonth={}",
                            businessException.getErrorCode().getCode(),
                            businessException.getErrorCode().getMessage(),
                            userId, yearMonth, businessException);
                    failedUserIds.add(userId);
                }
            }

            return new MonthlyReportAggregationResultInfo(yearMonth, true, targetUserIds.size(), succeeded, failedUserIds);
        } finally {
            lockHandle.get().close();
        }
    }


    private void aggregateForUser(Long userId, YearMonth yearMonth) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEndExclusive = yearMonth.plusMonths(1).atDay(1);

        List<ConsumptionActivity> monthActivities = consumptionActivityPort.findActivities(userId, monthStart, monthEndExclusive);

        if (monthActivities.isEmpty()) {
            log.info("이번 달 소비 활동이 없어 리포트를 생성하지 않습니다. userId={}, yearMonth={}", userId, yearMonth);
            return;
        }

        List<ConsumptionActivity> priorActivities = consumptionActivityPort.findActivities(userId, ACTIVITY_HISTORY_START, monthStart);

        List<Long> placeIds = Stream.concat(monthActivities.stream(), priorActivities.stream())
                .map(ConsumptionActivity::placeId)
                .distinct()
                .toList();
        Map<Long, String> dongNames = dongNameLookupPort.findDongNames(placeIds);
        Map<Long, String> placeNames = placeNameLookupPort.findPlaceNames(placeIds);

        //과거 방문 이력 집합 및 장소별 최초 방문일 계산
        Set<Long> priorVisitedPlaceIds = priorActivities.stream().map(ConsumptionActivity::placeId).collect(Collectors.toSet());

        Set<String> priorVisitedTownNames = priorActivities.stream()
                .map(activity -> dongNames.get(activity.placeId()))
                .filter(dongName -> dongName != null && !dongName.isBlank())
                .collect(Collectors.toSet());

        Map<Long, LocalDate> earliestVisitDateByPlaceId = Stream.concat(monthActivities.stream(), priorActivities.stream())
                .collect(Collectors.groupingBy(
                        ConsumptionActivity::placeId,
                        Collectors.mapping(ConsumptionActivity::purchaseDate,
                                Collectors.minBy(LocalDate::compareTo))
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

        List<MonthlyVisitActivity> monthVisitActivities = monthActivities.stream()
                .map(activity -> new MonthlyVisitActivity(
                        activity.placeId(),
                        dongNames.get(activity.placeId()),
                        placeNames.getOrDefault(activity.placeId(), UNKNOWN_PLACE_NAME),
                        activity.category(),
                        activity.purchaseDate(),
                        activity.purchaseTime()
                ))
                .toList();

        // 통계 계산
        MonthlyAggregationResult aggregation = monthlyAggregationCalculator.calculate(monthVisitActivities, priorVisitedPlaceIds, priorVisitedTownNames, earliestVisitDateByPlaceId);
        //페르소나 점수 산출
        PersonaScoreResult personaScore = personaScoringService.score(monthVisitActivities, priorVisitedPlaceIds, aggregation);

        replaceReport(userId, monthStart, aggregation, personaScore);
    }

    private void replaceReport(Long userId, LocalDate monthStart, MonthlyAggregationResult aggregation, PersonaScoreResult personaScore) {
        Optional<Report> existing = reportRepository.findByUserIdAndReportMonth(userId, monthStart);
        if (existing.isPresent()) {
            reportRepository.delete(existing.get());
            reportRepository.flush();
        }

        Report report = Report.builder()
                .userId(userId)
                .reportMonth(monthStart)
                .personaType(personaScore.personaType())
                .scoreExploration(personaScore.scoreExploration())
                .scoreTownExpansion(personaScore.scoreTownExpansion())
                .scoreDaytime(personaScore.scoreDaytime())
                .scoreImpulsive(personaScore.scoreImpulsive())
                .totalVisitCount(aggregation.totalVisitCount())
                .newTownCount(aggregation.newTownCount())
                .newPlaceCount(aggregation.newPlaceCount())
                .newStickerCount(aggregation.newPlaceCount())
                .build();
        reportRepository.save(report);

        Long reportId = report.getId();

        reportCategoryStatRepository.saveAll(aggregation.categoryStats().stream()
                .map(stat -> ReportCategoryStat.builder()
                        .reportId(reportId)
                        .category(stat.category())
                        .percentage(stat.percentage())
                        .build())
                .toList());

        reportTownRankRepository.saveAll(aggregation.townRanks().stream()
                .map(townRank -> ReportTownRank.builder()
                        .reportId(reportId)
                        .rank(townRank.rank())
                        .townName(townRank.townName())
                        .visitCount(townRank.visitCount())
                        .build())
                .toList());

        reportPlaceRankRepository.saveAll(aggregation.placeRanks().stream()
                .map(placeRank -> ReportPlaceRank.builder()
                        .reportId(reportId)
                        .rank(placeRank.rank())
                        .placeId(placeRank.placeId())
                        .placeName(placeRank.placeName())
                        .visitCount(placeRank.visitCount())
                        .firstVisitedDate(placeRank.firstVisitedDate())
                        .build())
                .toList());

        reportTimePatternRepository.saveAll(aggregation.timePatterns().stream()
                .map(timePattern -> ReportTimePattern.builder()
                        .reportId(reportId)
                        .dayOfWeek(timePattern.dayOfWeek())
                        .visitHour(timePattern.visitHour())
                        .visitCount(timePattern.visitCount())
                        .build())
                .toList());
    }

    private long lockKey(YearMonth yearMonth) {
        return LOCK_NAMESPACE * 100L + yearMonth.getYear() * 100L + yearMonth.getMonthValue();
    }
}
