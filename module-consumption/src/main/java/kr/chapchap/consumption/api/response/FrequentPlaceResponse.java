package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
            long visitCount,
            @Schema(
                    description = "Google Places 대표 사진의 단기 URL(장소당 최대 1장). 사진이 없거나 조회에 실패하면 null",
                    nullable = true
            )
            String thumbnailUrl
    ) {
        public static FrequentPlaceItem from(PlaceRankInfo info) {
            return new FrequentPlaceItem(
                    info.rank(), info.placeId(), info.placeName(), info.category(), info.dongName(), info.visitCount(),
                    info.thumbnailUrl()
            );
        }
    }
}
