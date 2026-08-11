package kr.chapchap.report.application.command;

import java.time.YearMonth;

public record AggregateMonthlyReportCommand(
        YearMonth yearMonth
) {

    public static AggregateMonthlyReportCommand forAllActiveUsers(YearMonth yearMonth) {
        return new AggregateMonthlyReportCommand(yearMonth);
    }
}
