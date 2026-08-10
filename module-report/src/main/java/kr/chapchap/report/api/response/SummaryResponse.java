package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.SummaryInfo;

public record SummaryResponse(
        int totalVisitCount,
        int newTownCount,
        int newPlaceCount
) {

    public static SummaryResponse from(SummaryInfo info) {
        return new SummaryResponse(info.totalVisitCount(), info.newTownCount(), info.newPlaceCount());
    }
}
