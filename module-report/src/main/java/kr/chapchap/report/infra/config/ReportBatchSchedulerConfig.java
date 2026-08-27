package kr.chapchap.report.infra.config;

import kr.chapchap.report.application.command.AggregateMonthlyReportCommand;
import kr.chapchap.report.application.info.MonthlyReportAggregationResultInfo;
import kr.chapchap.report.application.service.MonthlyReportAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ReportBatchSchedulerConfig {

    private final MonthlyReportAggregationService monthlyReportAggregationService;
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Seoul");
    private final Clock clock;

    @Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Seoul")
    public void aggregatePreviousMonth() {
        LocalDate today = LocalDate.now(clock.withZone(REPORT_ZONE));
        YearMonth targetMonth = YearMonth.from(today).minusMonths(1);

        log.info("월간 리포트 작업 시작. targetMonth={}", targetMonth);
        MonthlyReportAggregationResultInfo result = monthlyReportAggregationService.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(targetMonth));
        log.info("월간 리포트 작업 종료. result={}", result);
    }
}
