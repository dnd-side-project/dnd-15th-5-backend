package kr.chapchap.report.application.command;

import java.time.YearMonth;

public record GetMonthlyReportCommand(
        Long userId,
        YearMonth yearMonth
) {
}
