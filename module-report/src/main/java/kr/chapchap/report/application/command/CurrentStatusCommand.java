package kr.chapchap.report.application.command;

import java.time.YearMonth;

public record CurrentStatusCommand(
        Long userId,
        YearMonth yearMonth
) {
}
