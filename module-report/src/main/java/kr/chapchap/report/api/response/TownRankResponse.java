package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.TownRankInfo;

public record TownRankResponse(
        int rank,
        String townName,
        int visitCount
) {

    public static TownRankResponse from(TownRankInfo info) {
        return new TownRankResponse(info.rank(), info.townName(), info.visitCount());
    }
}
