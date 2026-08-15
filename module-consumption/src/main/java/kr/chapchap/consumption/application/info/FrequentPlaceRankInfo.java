package kr.chapchap.consumption.application.info;

import java.util.List;

public record FrequentPlaceRankInfo(
        List<PlaceRankInfo> places,
        boolean hasNext,
        Long nextCursorVisitCount,
        Long nextCursorPlaceId,
        int nextCursorRank
) {

    public record PlaceRankInfo(
            int rank,
            Long placeId,
            String placeName,
            String category,
            String dongName,
            long visitCount
    ) {
    }
}
