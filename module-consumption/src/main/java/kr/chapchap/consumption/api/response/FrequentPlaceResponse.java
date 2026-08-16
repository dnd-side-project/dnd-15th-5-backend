package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.FrequentPlaceRankInfo;
import kr.chapchap.consumption.application.info.FrequentPlaceRankInfo.PlaceRankInfo;

import java.util.List;

public record FrequentPlaceResponse(
        List<FrequentPlaceItem> places,
        boolean hasNext,
        Long nextCursorVisitCount,
        Long nextCursorPlaceId,
        int nextCursorRank
) {
    public static FrequentPlaceResponse from(FrequentPlaceRankInfo info) {
        List<FrequentPlaceItem> items = info.places().stream().map(FrequentPlaceItem::from).toList();
        return new FrequentPlaceResponse(
                items, info.hasNext(), info.nextCursorVisitCount(), info.nextCursorPlaceId(), info.nextCursorRank()
        );
    }

    public record FrequentPlaceItem(
            int rank,
            Long placeId,
            String placeName,
            String category,
            String dongname,
            long visitCount
    ) {
        public static FrequentPlaceItem from(PlaceRankInfo info) {
            return new FrequentPlaceItem(
                    info.rank(), info.placeId(), info.placeName(), info.category(), info.dongName(), info.visitCount()
            );
        }
    }
}
