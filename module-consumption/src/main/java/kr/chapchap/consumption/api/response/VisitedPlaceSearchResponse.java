package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo.VisitedPlaceInfo;

import java.util.List;

@Schema(description = "방문 장소 검색 결과")
public record VisitedPlaceSearchResponse(
        @Schema(description = "최근 방문순으로 정렬된 장소 목록")
        List<VisitedPlaceItem> places,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 페이지 조회용 커서, 다음 페이지가 없으면 null", nullable = true)
        String nextCursor
) {

    public static VisitedPlaceSearchResponse from(VisitedPlaceSearchInfo info) {
        return new VisitedPlaceSearchResponse(
                info.places().stream()
                        .map(VisitedPlaceItem::from)
                        .toList(),
                info.hasNext(),
                info.nextCursor()
        );
    }

    @Schema(description = "방문 장소 검색 항목")
    public record VisitedPlaceItem(
            @Schema(description = "장소 식별자", example = "1")
            Long placeId,

            @Schema(description = "장소명", example = "부첼리하우스 신논현점")
            String placeName,

            @Schema(description = "도로명주소", example = "서울특별시 강남구 봉은사로 125")
            String roadAddress,

            @Schema(
                    description = "목록에 표시할 Google Places 단기 사진 URL, 사진이 없거나 조회에 실패하면 null",
                    nullable = true
            )
            String thumbnailUrl,

            @Schema(
                    description = "같은 사진을 Google Maps에서 확인하는 페이지 URL. "
                            + "썸네일 클릭 또는 별도 링크로 해당 사진을 확인할 때 사용하며, 사진이 없으면 null",
                    nullable = true
            )
            String googleMapsUri
    ) {

        private static VisitedPlaceItem from(VisitedPlaceInfo info) {
            return new VisitedPlaceItem(
                    info.placeId(),
                    info.placeName(),
                    info.roadAddress(),
                    info.thumbnailUrl(),
                    info.googleMapsUri()
            );
        }
    }
}
