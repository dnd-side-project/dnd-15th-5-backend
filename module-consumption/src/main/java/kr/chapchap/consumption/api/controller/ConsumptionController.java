package kr.chapchap.consumption.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.consumption.api.response.ConsumptionScrollResponse;
import kr.chapchap.consumption.api.response.FrequentPlaceResponse;
import kr.chapchap.consumption.api.response.PlaceDetailResponse;
import kr.chapchap.consumption.api.response.PlaceVisitScrollResponse;
import kr.chapchap.consumption.api.response.VisitedPlaceMarkerResponse;
import kr.chapchap.consumption.application.command.ConsumptionSearchCommand;
import kr.chapchap.consumption.application.command.FrequentPlaceRankCommand;
import kr.chapchap.consumption.application.command.PlaceVisitSearchCommand;
import kr.chapchap.consumption.application.command.RankingPeriod;
import kr.chapchap.consumption.application.info.ConsumptionScrollInfo;
import kr.chapchap.consumption.application.info.FrequentPlaceRankInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo;
import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.service.ConsumptionQueryService;
import kr.chapchap.consumption.application.service.FrequentPlaceQueryService;
import kr.chapchap.consumption.application.service.PlaceDetailQueryService;
import kr.chapchap.consumption.application.service.VisitedPlaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final FrequentPlaceQueryService frequentPlaceQueryService;
    private final PlaceDetailQueryService placeDetailQueryService;

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
            @ChapChapUserId Long userId,
            @Parameter(description = "필터링할 카테고리 (생략시 전체 카테고리)")
            @RequestParam(required = false) List<String> categories
    ) {
        VisitedPlaceMarkersInfo info = visitedPlaceQueryService.getVisitedPlaceMarkers(userId, categories);
        return ApiResponse.success(VisitedPlaceMarkerResponse.from(info));
    }

    @Operation(
            summary = "자주 소비한 곳 랭킹 무한스크롤 조회",
            description = "방문 횟수 내림차순으로 장소 랭킹을 커서 기반으로 조회합니다. "
                    + "period [이번달/전체누적]  category로 필터링 가능 (생략시 전체 카테고리). "
                    + "nextCursorVisitCount/nextCursorPlaceId/nextCursorRank를 다음 요청에 그대로 넣어 보내면 됩니다."
    )
    @GetMapping("/places/rank")
    public ApiResponse<FrequentPlaceResponse> getFrequentPlaces(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회 기간, 기본값 THIS_MONTH") @RequestParam(defaultValue = "THIS_MONTH") RankingPeriod period,
            @Parameter(description = "필터링할 카테고리 (생략시 전체 카테고리)")
            @RequestParam(required = false) List<String> category,
            @Parameter(description = "이전 응답의 nextCursorVisitCount (첫 조회 시 생략)")
            @RequestParam(required = false) Long cursorVisitCount,
            @Parameter(description = "이전 응답의 nextCursorPlaceId (첫 조회 시 생략)")
            @RequestParam(required = false) Long cursorPlaceId,
            @Parameter(description = "이전 응답의 nextCursorRank (첫 조회 시 생략)")
            @RequestParam(defaultValue = "0") int cursorRank,
            @RequestParam(defaultValue = "10") int size
    ) {
        FrequentPlaceRankCommand command = new FrequentPlaceRankCommand(
                userId, period, category, cursorVisitCount, cursorPlaceId, cursorRank, size
        );
        FrequentPlaceRankInfo info = frequentPlaceQueryService.getFrequentPlaces(command);
        return ApiResponse.success(FrequentPlaceResponse.from(info));
    }

    @Operation(
            summary = "장소 상세 조회",
            description = "장소 정보, 단골(isRegular) 여부, 방문 통계, 스티커 정보를 조회합니다. 페이지네이션 없음."
    )
    @GetMapping("/places/{placeId}")
    public ApiResponse<PlaceDetailResponse> getPlaceDetail(
            @ChapChapUserId Long userId,
            @PathVariable Long placeId
    ) {
        PlaceDetailInfo info = placeDetailQueryService.getDetail(userId, placeId);
        return ApiResponse.success(PlaceDetailResponse.from(info));
    }

    @Operation(
            summary = "장소별 방문 이력 무한스크롤 조회",
            description = "장소 하나로 좁혀진 방문 이력을 최신순 커서 기반으로 조회합니다(월 제한 없음). "
                    + "nextCursorPurchaseDate/nextCursorPurchaseTime/nextCursorId를 다음 요청에 넣어 보내면 됩니다."
    )
    @GetMapping("/places/{placeId}/visits")
    public ApiResponse<PlaceVisitScrollResponse> getPlaceVisits(
            @ChapChapUserId Long userId,
            @PathVariable Long placeId,
            @Parameter(description = "이전 응답의 nextCursorPurchaseDate (첫 조회 시 생략)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cursorPurchaseDate,
            @Parameter(description = "이전 응답의 nextCursorPurchaseTime (첫 조회 시 생략)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime cursorPurchaseTime,
            @Parameter(description = "이전 응답의 nextCursorId (첫 조회 시 생략)")
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        PlaceVisitSearchCommand command = new PlaceVisitSearchCommand(
                userId, placeId, cursorPurchaseDate, cursorPurchaseTime, cursorId, size
        );
        PlaceVisitScrollInfo info = placeDetailQueryService.getVisits(command);
        return ApiResponse.success(PlaceVisitScrollResponse.from(info));
    }
}
