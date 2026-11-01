package kr.chapchap.place.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.place.api.request.UserTownSaveRequest;
import kr.chapchap.place.api.response.RecentTownsResponse;
import kr.chapchap.place.api.response.TownSearchResponse;
import kr.chapchap.place.api.response.UserTownResponse;
import kr.chapchap.place.application.service.TownSearchQueryService;
import kr.chapchap.place.application.service.UserTownCommandService;
import kr.chapchap.place.application.service.UserTownQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Town", description = "동네 검색 및 설정 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/towns")
public class TownController {

    private final TownSearchQueryService townSearchQueryService;
    private final UserTownCommandService userTownCommandService;
    private final UserTownQueryService userTownQueryService;

    @Operation(
            summary = "행정동 검색",
            description = "키워드로 행정동 후보를 검색합니다. 전국 행정동을 "
                    + "대상으로 검색하며, 좌표는 포함하지 않습니다(좌표 변환은 클라이언트에서 수행)."
    )
    @GetMapping("/search")
    public ApiResponse<TownSearchResponse> searchTowns(
            @Parameter(description = "검색 키워드", example = "성수") @RequestParam String keyword
    ) {
        return ApiResponse.success(TownSearchResponse.from(townSearchQueryService.search(keyword)));
    }

    @Operation(
            summary = "동네 설정하기",
            description = "검색 결과 중 선택한 행정동을 사용자의 동네로 저장합니다. 좌표는 클라이언트가 변환해서 "
                    + "함께 전달해야 하며, 이미 설정한 적 있는 행정동이면 좌표와 설정 시각만 갱신합니다. "
                    + "최근 설정한 동네는 최신 10개까지만 유지됩니다."
    )
    @PostMapping
    public ApiResponse<UserTownResponse> saveTown(
            @ChapChapUserId Long userId,
            @Valid @RequestBody UserTownSaveRequest request
    ) {
        return ApiResponse.success(UserTownResponse.from(userTownCommandService.save(request.toCommand(userId))));
    }

    @Operation(
            summary = "최근 설정한 동네 목록",
            description = "최근 설정한 동네를 최신순으로 조회합니다."
    )
    @GetMapping("/recent")
    public ApiResponse<RecentTownsResponse> getRecentTowns(
            @ChapChapUserId Long userId
    ) {
        return ApiResponse.success(RecentTownsResponse.from(userTownQueryService.findRecentTowns(userId)));
    }

    @Operation(
            summary = "최근 설정한 동네 삭제",
            description = "최근 설정한 동네 목록에서 하나를 삭제합니다."
    )
    @DeleteMapping("/recent/{userTownId}")
    public ApiResponse<Void> deleteRecentTown(
            @ChapChapUserId Long userId,
            @PathVariable Long userTownId
    ) {
        userTownCommandService.delete(userId, userTownId);
        return ApiResponse.ok();
    }
}
