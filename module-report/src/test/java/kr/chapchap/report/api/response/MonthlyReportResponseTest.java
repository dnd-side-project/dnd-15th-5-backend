package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.MonthlyReportInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MonthlyReportResponseTest {

    @Test
    void 시간_패턴이_하나도_없는_리포트는_peakDayOfWeek를_null로_변환한다() {
        // given: purchaseTime이 전부 없는 달 - MonthlyAggregationCalculator가 timePatterns를 빈 리스트로 만드는 경우
        MonthlyReportInfo info = new MonthlyReportInfo(
                1L,
                YearMonth.of(2026, 7),
                new MonthlyReportInfo.PersonaInfo("RHDP", "테스트", new MonthlyReportInfo.ScoresInfo(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                List.of(),
                List.of(),
                new MonthlyReportInfo.DiscoveryInfo("메시지", 0),
                new MonthlyReportInfo.SummaryInfo(3, 0, 0),
                List.of(),
                new MonthlyReportInfo.TimePatternInfo(0, 0, List.of())
        );

        // when & then
        assertThatCode(() -> MonthlyReportResponse.from(info)).doesNotThrowAnyException();

        MonthlyReportResponse response = MonthlyReportResponse.from(info);
        assertThat(response.timePattern().peakDayOfWeek()).isNull();
    }
}
