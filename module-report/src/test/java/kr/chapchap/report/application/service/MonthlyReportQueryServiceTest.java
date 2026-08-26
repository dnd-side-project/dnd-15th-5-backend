package kr.chapchap.report.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.PlaceStickerLookupPort;
import kr.chapchap.report.application.port.UserNicknameLookupPort;
import kr.chapchap.report.domain.entity.PersonaType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportQueryServiceTest {

    private static final Long REPORT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 7);

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
    private ConsumptionActivityPort consumptionActivityPort;

    @Mock
    private PlaceStickerLookupPort placeStickerLookupPort;

    @Mock
    private UserNicknameLookupPort userNicknameLookupPort;

    private MonthlyReportQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new MonthlyReportQueryService(
                reportRepository,
                reportCategoryStatRepository,
                reportTownRankRepository,
                reportPlaceRankRepository,
                reportTimePatternRepository,
                consumptionActivityPort,
                placeStickerLookupPort,
                userNicknameLookupPort
        );
    }

    @Test
    void 존재하지_않는_연월로_조회하면_REPORT_NOT_FOUND_예외가_발생한다() {
        // given
        when(reportRepository.findByUserIdAndReportMonth(USER_ID, YEAR_MONTH.atDay(1)))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.getMonthlyReport(new GetMonthlyReportCommand(USER_ID, YEAR_MONTH)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ReportErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void 저장된_리포트를_조회하면_페르소나와_랭킹_정보를_포함해_반환한다() {
        // given
        Report report = createReportWithId();
        when(reportRepository.findByUserIdAndReportMonth(USER_ID, YEAR_MONTH.atDay(1)))
                .thenReturn(Optional.of(report));
        when(reportCategoryStatRepository.findByReportId(REPORT_ID)).thenReturn(List.of(
                ReportCategoryStat.builder().reportId(REPORT_ID).category("카페").percentage(BigDecimal.valueOf(60)).build()
        ));
        when(reportTownRankRepository.findByReportIdOrderByRankAsc(REPORT_ID)).thenReturn(List.of(
                ReportTownRank.builder().reportId(REPORT_ID).rank(1).townName("연남동").visitCount(5).build()
        ));
        when(reportPlaceRankRepository.findByReportIdOrderByRankAsc(REPORT_ID)).thenReturn(List.of(
                ReportPlaceRank.builder().reportId(REPORT_ID).rank(1).placeId(101L).placeName("투썸 플레이스 뚝섬지점")
                        .visitCount(9).firstVisitedDate(LocalDate.of(2026, 5, 1)).build()
        ));
        when(reportTimePatternRepository.findByReportId(REPORT_ID)).thenReturn(List.of());
        when(consumptionActivityPort.findActivities(eq(USER_ID), any(), any())).thenReturn(List.of(
                new ConsumptionActivity(101L, "카페", LocalDate.of(2026, 7, 3), null)
        ));
        when(placeStickerLookupPort.findRecentStickerNames(eq(USER_ID), eq(101L), any(), any(), anyInt()))
                .thenReturn(List.of("도넛", "아이스"));

        // when
        MonthlyReportInfo info = sut.getMonthlyReport(new GetMonthlyReportCommand(USER_ID, YEAR_MONTH));

        // then
        assertThat(info.reportId()).isEqualTo(REPORT_ID);
        assertThat(info.persona().type()).isEqualTo("RHMP");
        assertThat(info.persona().typeName()).isEqualTo("단골 반복형 · 동네 집중형 · 밤소비형 · 규칙형");
        assertThat(info.persona().keywords()).containsExactly("단골 반복형", "동네 집중형", "밤소비형", "규칙형");
        assertThat(info.placeRanks()).hasSize(1);
        assertThat(info.placeRanks().get(0).placeName()).isEqualTo("투썸 플레이스 뚝섬지점");
        assertThat(info.placeRanks().get(0).category()).isEqualTo("카페");
        assertThat(info.placeRanks().get(0).stickerNames()).containsExactly("도넛", "아이스");
        assertThat(info.townRanks()).hasSize(1);
        assertThat(info.summary().totalVisitCount()).isEqualTo(24);
    }

    @Test
    void 요일별_시간대별_방문건수가_가장_많은_조합을_피크로_반환한다() {
        // given
        Report report = createReportWithId();
        when(reportRepository.findByUserIdAndReportMonth(USER_ID, YEAR_MONTH.atDay(1)))
                .thenReturn(Optional.of(report));
        when(reportCategoryStatRepository.findByReportId(REPORT_ID)).thenReturn(List.of());
        when(reportTownRankRepository.findByReportIdOrderByRankAsc(REPORT_ID)).thenReturn(List.of());
        when(reportPlaceRankRepository.findByReportIdOrderByRankAsc(REPORT_ID)).thenReturn(List.of());
        when(reportTimePatternRepository.findByReportId(REPORT_ID)).thenReturn(List.of(
                ReportTimePattern.builder().reportId(REPORT_ID).dayOfWeek(3).visitHour(19).visitCount(3).build(),
                ReportTimePattern.builder().reportId(REPORT_ID).dayOfWeek(5).visitHour(22).visitCount(8).build(),
                ReportTimePattern.builder().reportId(REPORT_ID).dayOfWeek(6).visitHour(21).visitCount(4).build()
        ));

        // when
        MonthlyReportInfo info = sut.getMonthlyReport(new GetMonthlyReportCommand(USER_ID, YEAR_MONTH));

        // then
        assertThat(info.timePattern().peakDayOfWeek()).isEqualTo(5);
        assertThat(info.timePattern().peakTimeSlot()).isEqualTo("NIGHT");
        assertThat(info.timePattern().dayOfWeekPattern()).hasSize(7);
        assertThat(info.timePattern().dayOfWeekPattern().get(4).visitCount()).isEqualTo(8); // 5번째 = 금요일
    }

    private Report createReportWithId() {
        Report report = Report.builder()
                .userId(USER_ID)
                .reportMonth(YEAR_MONTH.atDay(1))
                .personaType(PersonaType.RHMP)
                .scoreExploration(BigDecimal.valueOf(0.3))
                .scoreTownExpansion(BigDecimal.valueOf(0.2))
                .scoreDaytime(BigDecimal.valueOf(0.1))
                .scoreImpulsive(BigDecimal.valueOf(0.8))
                .totalVisitCount(24)
                .newTownCount(5)
                .newPlaceCount(8)
                .newStickerCount(3)
                .build();
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        return report;
    }
}
