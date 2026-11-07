package kr.chapchap.report.application.command;

import java.time.LocalDate;
import java.time.YearMonth;

public record GetReportCalendarCommand(
        Long userId,
        YearMonth yearMonth,
        LocalDate weekAnchor
) {
}
