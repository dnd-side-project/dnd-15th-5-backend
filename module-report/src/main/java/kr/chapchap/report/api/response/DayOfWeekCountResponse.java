package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.DayOfWeekCountInfo;

public record DayOfWeekCountResponse(
        int dayOfWeek,
        int visitCount
) {

    public static DayOfWeekCountResponse from(DayOfWeekCountInfo info) {
        return new DayOfWeekCountResponse(info.dayOfWeek(), info.visitCount());
    }
}
