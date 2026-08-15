package kr.chapchap.consumption.application.info;

import java.util.List;

public record FrequentPlaceRankInfo(
        List<FrequentPlaceInfo> places,
        boolean hasNext,
        Long nextCursorVisitCount,
        Long nextCursorPlaceId,
        int nextCursorRank
) {
}
