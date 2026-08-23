package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.consumption.api.response.VisitedPlaceSearchResponse;
import kr.chapchap.consumption.application.command.VisitedPlaceSearchCommand;
import kr.chapchap.consumption.application.info.VisitedPlaceSearchInfo;
import kr.chapchap.consumption.application.service.VisitedPlaceSearchService;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Place", description = "장소 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/places")
public class VisitedPlaceSearchController {

    private final VisitedPlaceSearchService visitedPlaceSearchService;

    @Operation(
            summary = "방문 장소 검색",
            description = "현재 사용자가 소비 기록을 남긴 장소를 이름과 주소로 검색하여 최근 방문순으로 조회합니다."
    )
    @GetMapping("/visited/search")
    public ApiResponse<VisitedPlaceSearchResponse> searchVisitedPlaces(
            @ChapChapUserId Long userId,
            @RequestParam String keyword,
            @Parameter(description = "이전 응답의 nextCursor, 첫 조회 시 생략")
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") int size
    ) {
        VisitedPlaceSearchCommand command = new VisitedPlaceSearchCommand(userId, keyword, cursor, size);
        VisitedPlaceSearchInfo info = visitedPlaceSearchService.search(command);
        return ApiResponse.success(VisitedPlaceSearchResponse.from(info));
    }
}
