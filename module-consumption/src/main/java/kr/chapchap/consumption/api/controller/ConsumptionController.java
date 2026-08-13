package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.consumption.api.response.ConsumptionScrollResponse;
import kr.chapchap.consumption.api.response.VisitedPlaceMarkerResponse;
import kr.chapchap.consumption.application.command.ConsumptionSearchCommand;
import kr.chapchap.consumption.application.info.ConsumptionScrollInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.consumption.application.service.VisitedPlaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consumption", description = "소비내역 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/consumptions")
public class ConsumptionController {

    private final ConsumptionQueryService consumptionQueryService;
    private final VisitedPlaceQueryService visitedPlaceQueryService;

    @Operation(
            summary = "월별 소비내역 무한스크롤 조회",
            description = "계정과 연월(yyyy-MM) 기준으로 소비내역을 최신순 커서 기반으로 조회합니다. "
                    + "nextCursorPurchaseDate/nextCursorPurchaseTime/nextCursorId를 다음 요청에 넣어 보내면 됩니다."
    )
    @GetMapping
    public ApiResponse<ConsumptionScrollResponse> getConsumptions(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회할 연월, 예: 2026-07") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @Parameter(description = "이전 응답의 nextCursorPurchaseDate (첫 조회 시 생략)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cursorPurchaseDate,
            @Parameter(description = "이전 응답의 nextCursorPurchaseTime (첫 조회 시 생략)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime cursorPurchaseTime,
            @Parameter(description = "이전 응답의 nextCursorId (첫 조회 시 생략)")
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        ConsumptionSearchCommand command = new ConsumptionSearchCommand(userId, yearMonth, cursorPurchaseDate, cursorPurchaseTime, cursorId, size);
        ConsumptionScrollInfo info = consumptionQueryService.search(command);
        return ApiResponse.success(ConsumptionScrollResponse.from(info));
    }

    @Operation(
            summary = "홈 화면 지도용 방문 장소 마커 조회",
            description = "현재 사용자가 소비기록을 남긴 장소 전체를 위경도 좌표와 함께 조회합니다. "
                    + "페이지네이션 없이 전체 목록을 반환합니다 categories 파라미터로 특정 카테고리만 필터링할 수 있습니다."
    )
    @GetMapping("/visited-places")
    public ApiResponse<VisitedPlaceMarkerResponse> getVisitedPlaceMarkers(
//            @ChapChapUserId Long userId,
            @Parameter(description = "필터링할 카테고리 (생략시 전체 카테고리)")
            @RequestParam(required = false) List<String> categories
    ) {
        VisitedPlaceMarkersInfo info = visitedPlaceQueryService.getVisitedPlaceMarkers(1L, categories);
        return ApiResponse.success(VisitedPlaceMarkerResponse.from(info));
    }
}
