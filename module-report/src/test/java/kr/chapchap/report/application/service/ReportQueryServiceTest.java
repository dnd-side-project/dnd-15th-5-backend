package kr.chapchap.report.application.service;

import kr.chapchap.report.application.command.CurrentStatusCommand;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.CurrentStatusInfo;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.DongNameLookupPort;
import kr.chapchap.report.domain.service.RecentDiscoveryMessageGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-08-09T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private ConsumptionActivityPort consumptionActivityPort;

    @Mock
    private DongNameLookupPort dongNameLookupPort;

    private ReportQueryService sut;

    @BeforeEach
    void setUp() {
        // @Mock 필드는 각 테스트 실행 전 Mockito가 주입해주므로, sut 생성은 필드 초기화가 아니라 여기서 해야 한다.
        sut = new ReportQueryService(
                consumptionActivityPort,
                dongNameLookupPort,
                new RecentDiscoveryMessageGenerator(),
                fixedClock
        );
    }

    @Test
    void 이번_달_소비건수는_요청한_연월에_해당하는_활동만_센다() {
        // given
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 8);

        when(consumptionActivityPort.findActivities(eq(userId), any(), any()))
                .thenReturn(List.of(
                        new ConsumptionActivity(100L, "카페", LocalDate.of(2026, 8, 1), LocalTime.of(12, 0)),
                        new ConsumptionActivity(101L, "음식점", LocalDate.of(2026, 8, 5), LocalTime.of(20, 0)),
                        new ConsumptionActivity(102L, "카페", LocalDate.of(2026, 7, 30), LocalTime.of(9, 0)) // 7월 → 제외돼야 함
                ));
        when(dongNameLookupPort.findDongNames(any())).thenReturn(Map.of());

        // when
        CurrentStatusInfo result = sut.getCurrentStatus(new CurrentStatusCommand(userId, yearMonth));

        // then
        assertThat(result.monthlyCount()).isEqualTo(2);
    }

    @Test
    void 이번_달_카테고리별_소비건수를_건수_많은_순으로_반환한다() {
        // given
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 8);

        when(consumptionActivityPort.findActivities(eq(userId), any(), any()))
                .thenReturn(List.of(
                        new ConsumptionActivity(100L, "카페", LocalDate.of(2026, 8, 1), LocalTime.of(12, 0)),
                        new ConsumptionActivity(101L, "카페", LocalDate.of(2026, 8, 2), LocalTime.of(12, 0)),
                        new ConsumptionActivity(102L, "음식점", LocalDate.of(2026, 8, 3), LocalTime.of(19, 0)),
                        new ConsumptionActivity(103L, "카페", LocalDate.of(2026, 7, 30), LocalTime.of(9, 0)) // 7월 → 제외돼야 함
                ));
        when(dongNameLookupPort.findDongNames(any())).thenReturn(Map.of());

        // when
        CurrentStatusInfo result = sut.getCurrentStatus(new CurrentStatusCommand(userId, yearMonth));

        // then
        assertThat(result.monthlyCategoryCounts())
                .containsExactly(Map.entry("카페", 2), Map.entry("음식점", 1));
    }

    @Test
    void 오늘이_일요일이면_이번_주는_오늘_하루뿐이라_그_전날_활동은_주간_카운트에서_빠진다() {
        // given
        Long userId = 1L;
        YearMonth yearMonth = YearMonth.of(2026, 8);

        when(consumptionActivityPort.findActivities(eq(userId), any(), any()))
                .thenReturn(List.of(
                        new ConsumptionActivity(200L, "카페", LocalDate.of(2026, 8, 9), LocalTime.of(10, 0)), // 오늘(일)
                        new ConsumptionActivity(201L, "카페", LocalDate.of(2026, 8, 8), LocalTime.of(10, 0))  // 어제(토, 지난주)
                ));
        when(dongNameLookupPort.findDongNames(any())).thenReturn(Map.of());

        // when
        CurrentStatusInfo result = sut.getCurrentStatus(new CurrentStatusCommand(userId, yearMonth));

        // then
        assertThat(result.weeklyCounts()).containsExactly(1, 0, 0, 0, 0, 0, 0);
    }
}
