package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.CurrentStatusInfo;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public record CurrentStatusResponse(
        String date,
        List<Integer> weeklyCounts,
        int monthlyCount,
        Map<String, Integer> monthlyCategoryCounts,
        String recentDiscoveryMessage,
        List<String> monthlyStickerNames
) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public static CurrentStatusResponse from(CurrentStatusInfo info) {
        return new CurrentStatusResponse(
                info.yearMonth().format(DATE_FORMAT),
                info.weeklyCounts(),
                info.monthlyCount(),
                info.monthlyCategoryCounts(),
                info.recentDiscoveryMessage(),
                info.monthlyStickerNames()
        );
    }
}
