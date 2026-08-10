package kr.chapchap.report.application.info;

import java.util.List;

public record TimePatternInfo(
        int peakDayOfWeek,
        int peakHour,
        List<DayOfWeekCountInfo> dayOfWeekPattern
) {
}
