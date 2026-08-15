package kr.chapchap.recommendation.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.recommendation.api.response.NearbyPlacesResponse;
import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.service.RecommendationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recommendation", description = "가게 추천 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationQueryService recommendationQueryService;

    @Operation(
            summary = "내 주변 가게 추천",
            description = "지도 중심 좌표 반경 안의 인기 가게를 조회. myTownPlaces는 전체 인기순, "
                    + "sameCategoryPlaces는 내가 가장 많이 방문한 카테고리로 필터링한 결과로 둘 중 중복된건 1개만 응 답"
    )
    @GetMapping("/nearby-places")
    public ApiResponse<NearbyPlacesResponse> getNearbyPlaces(
            @ChapChapUserId Long userId,
            @Parameter(description = "위도") @RequestParam double lat,
            @Parameter(description = "경도") @RequestParam double lng,
            @Parameter(description = "검색 반경(미터), 생략시 1500m / 위치기반이면 적정범위 500m~1km") @RequestParam(defaultValue = "1500") double radiusMeters
    ) {
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(userId, lat, lng, radiusMeters);
        return ApiResponse.success(NearbyPlacesResponse.from(info));
    }
}
