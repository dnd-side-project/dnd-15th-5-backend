package kr.chapchap.place.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.place.api.response.PlaceLikeResponse;
import kr.chapchap.place.api.response.LikedPlaceScrollResponse;
import kr.chapchap.place.application.command.LikedPlaceSearchCommand;
import kr.chapchap.place.application.service.PlaceLikeCommandService;
import kr.chapchap.place.application.service.PlaceLikeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Place", description = "장소 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/places")
public class PlaceController {

    private final PlaceLikeCommandService placeLikeCommandService;
    private final PlaceLikeQueryService placeLikeQueryService;

    @Operation(
            summary = "좋아요한 가게 목록",
            description = "최근 좋아요순으로 조회합니다. category를 생략하면 전체를 조회합니다. "
                    + "categories는 페이지·필터와 무관하게 내 좋아요 전체에 존재하는 대표 카테고리입니다. "
                    + "대표 카테고리는 전체 소비기록 수 기준이며 동률이면 이름순입니다. 기록이 없으면 null입니다. "
                    + "필터를 바꾸면 cursorId를 생략하고 처음부터 조회합니다. 사진은 장소당 최대 3개입니다."
    )
    @GetMapping("/likes")
    public ApiResponse<LikedPlaceScrollResponse> getLikedPlaces(
            @ChapChapUserId Long userId,
            @RequestParam(required = false) String category,
            @Parameter(description = "이전 응답의 nextCursorId (첫 조회 시 생략)")
            @RequestParam(required = false) Long cursorId,
            @Parameter(description = "조회 개수 (1~5)")
            @RequestParam(defaultValue = "5") int size
    ) {
        return ApiResponse.success(LikedPlaceScrollResponse.from(placeLikeQueryService.findLikedPlaces(
                new LikedPlaceSearchCommand(userId, category, cursorId, size)
        )));
    }

    @Operation(
            summary = "가게 좋아요 토글",
            description = "좋아요 상태를 등록하고 취소합니다. 동시 등록·해제 충돌은 409(PLACE007)를 반환합니다."
    )
    @PutMapping("/{placeId}/likes")
    public ApiResponse<PlaceLikeResponse> toggleLike(
            @ChapChapUserId Long userId,
            @PathVariable Long placeId
    ) {
        boolean liked = placeLikeCommandService.toggle(userId, placeId);
        return ApiResponse.success(new PlaceLikeResponse(liked));
    }
}
