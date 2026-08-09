package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.CurrentStatusInfo;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record CurrentStatusResponse(
        String date,
        List<Integer> weeklyCounts,
        int monthlyCount,
        String recentDiscoveryMessage
) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public static CurrentStatusResponse from(CurrentStatusInfo info) {
        return new CurrentStatusResponse(
                info.yearMonth().format(DATE_FORMAT),
                info.weeklyCounts(),
                info.monthlyCount(),
                info.recentDiscoveryMessage()
        );
    }
}
