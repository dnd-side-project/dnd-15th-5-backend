package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.TimePatternInfo;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;

public record TimePatternResponse(
        String peakDayOfWeek,
        int peakHour,
        List<DayOfWeekCountResponse> dayOfWeekPattern
) {

    public static TimePatternResponse from(TimePatternInfo info) {
        return new TimePatternResponse(
                toDayOfWeekCode(info.peakDayOfWeek()),
                info.peakHour(),
                info.dayOfWeekPattern().stream().map(DayOfWeekCountResponse::from).toList()
        );
    }

    // 1(월요일)~7(일요일)
    private static String toDayOfWeekCode(int dayOfWeek) {
        return DayOfWeek.of(dayOfWeek).name().substring(0, 3).toUpperCase(Locale.ROOT);
    }
}
