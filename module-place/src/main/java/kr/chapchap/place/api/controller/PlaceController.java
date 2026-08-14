package kr.chapchap.place.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.place.api.response.PlaceLikeResponse;
import kr.chapchap.place.application.service.PlaceLikeCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Place", description = "장소 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/places")
public class PlaceController {

    private final PlaceLikeCommandService placeLikeCommandService;

    @Operation(
            summary = "가게 좋아요 토글",
            description = "좋아요 상태를 등록하고 취소한다"
    )
    @PutMapping("/{placeId}/likes")
    public ApiResponse<PlaceLikeResponse> toggleLike(
//            @ChapChapUserId Long userId,
            @PathVariable Long placeId
    ) {
        boolean liked = placeLikeCommandService.toggle(1L, placeId);
        return ApiResponse.success(new PlaceLikeResponse(liked));
    }
}
