package kr.chapchap.recommendation.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.recommendation.api.response.NearbyPlacesResponse;
import kr.chapchap.recommendation.api.response.RecommendedPlaceDetailResponse;
import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.info.RecommendedPlaceDetailInfo;
import kr.chapchap.recommendation.application.service.RecommendationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recommendation", description = "가게 추천 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/recommendations")
public class RecommendationController {

    private final RecommendationQueryService recommendationQueryService;

    @Operation(
            summary = "내 주변 가게 추천",
            description = "지도 중심 좌표 반경 안의 인기 가게를 조회. categoryPlaces는 카테고리 전체 목록 각각에 대해 인기순 상위 4개씩 묶어서 응답하고"
                    + "(해당 카테고리에 후보가 없으면 빈 배열), allPlaces는 그 카테고리별 추천을 모두 합친 것(최대 카테고리 수 × 4개)을 인기순으로 응답"
    )
    @GetMapping("/nearby-places")
    public ApiResponse<NearbyPlacesResponse> getNearbyPlaces(
            @ChapChapUserId Long userId,
            @Parameter(description = "위도") @RequestParam double lat,
            @Parameter(description = "경도") @RequestParam double lng,
            @Parameter(description = "검색 반경(미터), 생략시 5000m ") @RequestParam(defaultValue = "5000") double radiusMeters
    ) {
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(userId, lat, lng, radiusMeters);
        return ApiResponse.success(NearbyPlacesResponse.from(info));
    }

    @Operation(
            summary = "가게 추천 상세보기",
            description = "추천 목록에서 선택한 가게의 이름, 카테고리, 위치와 Google 사진 최대 3장, 실제 Google 지도 링크를 조회"
    )
    @GetMapping("/places/{placeId}")
    public ApiResponse<RecommendedPlaceDetailResponse> getPlaceDetail(
            @PathVariable Long placeId
    ) {
        RecommendedPlaceDetailInfo info = recommendationQueryService.getPlaceDetail(placeId);
        return ApiResponse.success(RecommendedPlaceDetailResponse.from(info));
    }
}
