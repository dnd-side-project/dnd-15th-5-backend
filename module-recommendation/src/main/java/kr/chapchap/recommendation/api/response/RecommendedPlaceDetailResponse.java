package kr.chapchap.recommendation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.recommendation.application.info.RecommendedPlaceDetailInfo;

import java.util.List;

public record RecommendedPlaceDetailResponse(
        Long placeId,
        @Schema(description = "Google Place ID. 구글 지도 딥링크(예: maps/place/?q=place_id:{googlePlaceId}) 생성에 사용")
        String googlePlaceId,
        String name,
        String category,
        String dongName,
        @Schema(description = "도로명주소")
        String roadAddress,
        Double latitude,
        Double longitude,
        @Schema(description = "Google Places 사진의 URL(장소당 최대 3장). 사진이 없거나 조회에 실패하면 빈 배열")
        List<String> thumbnailUrls,
        @Schema(description = "사진 페이지 링크. 사진 조회에 실패하면 null", nullable = true)
        String googleMapsUri
) {
    public static RecommendedPlaceDetailResponse from(RecommendedPlaceDetailInfo info) {
        return new RecommendedPlaceDetailResponse(
                info.placeId(), info.googlePlaceId(), info.name(), info.category(), info.dongName(), info.roadAddress(),
                info.latitude(), info.longitude(), info.thumbnailUrls(), info.googleMapsUri()
        );
    }
}
