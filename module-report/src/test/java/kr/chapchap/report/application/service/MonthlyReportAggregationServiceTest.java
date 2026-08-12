package kr.chapchap.report.application.service;

import kr.chapchap.report.application.command.AggregateMonthlyReportCommand;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.MonthlyReportAggregationResultInfo;
import kr.chapchap.report.application.port.AdvisoryLockHandle;
import kr.chapchap.report.application.port.AdvisoryLockPort;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.DongNameLookupPort;
import kr.chapchap.report.application.port.PlaceNameLookupPort;
import kr.chapchap.report.domain.entity.PersonaScoreResult;
import kr.chapchap.report.domain.entity.PersonaType;
import kr.chapchap.report.domain.entity.Report;
import kr.chapchap.report.domain.repository.ReportCategoryStatRepository;
import kr.chapchap.report.domain.repository.ReportPlaceRankRepository;
import kr.chapchap.report.domain.repository.ReportRepository;
import kr.chapchap.report.domain.repository.ReportTimePatternRepository;
import kr.chapchap.report.domain.repository.ReportTownRankRepository;
import kr.chapchap.report.domain.service.MonthlyAggregationCalculator;
import kr.chapchap.report.domain.service.PersonaScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportAggregationServiceTest {

    @Mock
    private ConsumptionActivityPort consumptionActivityPort;
    @Mock
    private DongNameLookupPort dongNameLookupPort;
    @Mock
    private PlaceNameLookupPort placeNameLookupPort;
    @Mock
    private PersonaScoringService personaScoringService;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportCategoryStatRepository reportCategoryStatRepository;
    @Mock
    private ReportTownRankRepository reportTownRankRepository;
    @Mock
    private ReportPlaceRankRepository reportPlaceRankRepository;
    @Mock
    private ReportTimePatternRepository reportTimePatternRepository;
    @Mock
    private AdvisoryLockPort advisoryLockPort;
    @Mock
    private PlatformTransactionManager transactionManager;

    private MonthlyReportAggregationService sut;

    @BeforeEach
    void setUp() {
        // 락 획득에 실패하는 테스트는 perUserTransactionTemplate까지 안 가서 이 스텁을 안 쓴다 - lenient로 완화
        TransactionStatus status = mock(TransactionStatus.class);
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);

        sut = new MonthlyReportAggregationService(
                consumptionActivityPort,
                dongNameLookupPort,
                placeNameLookupPort,
                new MonthlyAggregationCalculator(),
                personaScoringService,
                reportRepository,
                reportCategoryStatRepository,
                reportTownRankRepository,
                reportPlaceRankRepository,
                reportTimePatternRepository,
                advisoryLockPort,
                transactionManager
        );
    }

    @Test
    void 락_획득에_실패하면_아무_사용자도_처리하지_않는다() {
        // given
        when(advisoryLockPort.tryLock(anyLong())).thenReturn(Optional.empty());

        // when
        MonthlyReportAggregationResultInfo result =
                sut.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(YearMonth.of(2026, 7)));

        // then
        assertThat(result.lockAcquired()).isFalse();
        verifyNoInteractions(consumptionActivityPort);
    }

    @Test
    void 기존_리포트가_있으면_삭제한_뒤_새로_저장한다() {
        // given
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 7);
        LocalDate monthStart = yearMonth.atDay(1);

        when(advisoryLockPort.tryLock(anyLong())).thenReturn(Optional.of(mock(AdvisoryLockHandle.class)));
        when(consumptionActivityPort.findActiveUserIds(monthStart, monthStart.plusMonths(1))).thenReturn(List.of(userId));

        ConsumptionActivity activity = new ConsumptionActivity(101L, "CAFE", LocalDate.of(2026, 7, 5), LocalTime.of(10, 0));
        when(consumptionActivityPort.findActivities(userId, monthStart, monthStart.plusMonths(1))).thenReturn(List.of(activity));
        when(consumptionActivityPort.findActivities(eq(userId), any(), eq(monthStart))).thenReturn(List.of());
        when(dongNameLookupPort.findDongNames(any())).thenReturn(Map.of(101L, "성수동"));
        when(placeNameLookupPort.findPlaceNames(any())).thenReturn(Map.of(101L, "테스트 가게"));
        when(personaScoringService.score(any(), any(), any())).thenReturn(
                new PersonaScoreResult(PersonaType.NWDF, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        Report existing = Report.builder()
                .userId(userId).reportMonth(monthStart).personaType(PersonaType.NWDF)
                .totalVisitCount(1).newTownCount(0).newPlaceCount(0).newStickerCount(0)
                .build();
        when(reportRepository.findByUserIdAndReportMonth(userId, monthStart)).thenReturn(Optional.of(existing));

        // when
        MonthlyReportAggregationResultInfo result =
                sut.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(yearMonth));

        // then
        assertThat(result.succeededCount()).isEqualTo(1);
        assertThat(result.failedUserIds()).isEmpty();
        verify(reportRepository).delete(existing);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void 이번_달_소비_활동이_없는_사용자는_리포트를_생성하지_않는다() {
        // given
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 7);
        LocalDate monthStart = yearMonth.atDay(1);

        when(advisoryLockPort.tryLock(anyLong())).thenReturn(Optional.of(mock(AdvisoryLockHandle.class)));
        when(consumptionActivityPort.findActiveUserIds(monthStart, monthStart.plusMonths(1))).thenReturn(List.of(userId));
        when(consumptionActivityPort.findActivities(userId, monthStart, monthStart.plusMonths(1))).thenReturn(List.of());

        // when
        MonthlyReportAggregationResultInfo result =
                sut.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(yearMonth));

        // then
        assertThat(result.succeededCount()).isEqualTo(1);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void 한_사용자_처리가_실패해도_나머지_사용자는_계속_처리한다() {
        // given
        Long failingUserId = 1L;
        Long succeedingUserId = 2L;
        YearMonth yearMonth = YearMonth.of(2026, 7);
        LocalDate monthStart = yearMonth.atDay(1);

        when(advisoryLockPort.tryLock(anyLong())).thenReturn(Optional.of(mock(AdvisoryLockHandle.class)));
        when(consumptionActivityPort.findActiveUserIds(monthStart, monthStart.plusMonths(1)))
                .thenReturn(List.of(failingUserId, succeedingUserId));

        when(consumptionActivityPort.findActivities(eq(failingUserId), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        ConsumptionActivity activity = new ConsumptionActivity(101L, "CAFE", LocalDate.of(2026, 7, 5), LocalTime.of(10, 0));
        when(consumptionActivityPort.findActivities(succeedingUserId, monthStart, monthStart.plusMonths(1)))
                .thenReturn(List.of(activity));
        when(consumptionActivityPort.findActivities(eq(succeedingUserId), any(), eq(monthStart)))
                .thenReturn(List.of());
        when(dongNameLookupPort.findDongNames(any())).thenReturn(Map.of());
        when(placeNameLookupPort.findPlaceNames(any())).thenReturn(Map.of());
        when(personaScoringService.score(any(), any(), any())).thenReturn(
                new PersonaScoreResult(PersonaType.NWDF, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportRepository.findByUserIdAndReportMonth(eq(succeedingUserId), any())).thenReturn(Optional.empty());

        // when
        MonthlyReportAggregationResultInfo result =
                sut.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(yearMonth));

        // then
        assertThat(result.succeededCount()).isEqualTo(1);
        assertThat(result.failedUserIds()).containsExactly(failingUserId);
    }
}
