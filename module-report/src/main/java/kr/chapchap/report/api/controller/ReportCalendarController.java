package kr.chapchap.report.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.report.api.response.CategoryDistributionResponse;
import kr.chapchap.report.api.response.ReportCalendarResponse;
import kr.chapchap.report.api.response.StickerBoardResponse;
import kr.chapchap.report.api.response.SummaryHighlightResponse;
import kr.chapchap.report.application.command.GetReportCalendarCommand;
import kr.chapchap.report.application.info.CategoryDistributionInfo;
import kr.chapchap.report.application.info.ReportCalendarInfo;
import kr.chapchap.report.application.info.StickerBoardInfo;
import kr.chapchap.report.application.info.SummaryHighlightInfo;
import kr.chapchap.report.application.service.ReportCalendarQueryService;
import kr.chapchap.report.application.service.ReportCategoryDistributionQueryService;
import kr.chapchap.report.application.service.ReportStickerBoardQueryService;
import kr.chapchap.report.application.service.ReportSummaryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Report", description = "리포트 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v2/reports")
public class ReportCalendarController {

    private final ReportCalendarQueryService reportCalendarQueryService;
    private final ReportCategoryDistributionQueryService reportCategoryDistributionQueryService;
    private final ReportStickerBoardQueryService reportStickerBoardQueryService;
    private final ReportSummaryQueryService reportSummaryQueryService;

    @Operation(
            summary = "리포트 캘린더 조회",
            description = "연월(yyyy-MM) 기준 월별 캘린더, weekAnchor(해당 날짜가 속한 일~토)를 넘기면 주간 캘린더를 조회한다. "
                    + "날짜별 소비기록 전체 목록을 반환한다."
    )
    @GetMapping("/calendar")
    public ApiResponse<ReportCalendarResponse> getCalendar(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회할 연월, 예: 2026-07") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @Parameter(description = "주간 캘린더 조회 시 기준 날짜(해당 날짜가 속한 일~토 7일 반환), 예: 2026-07-13")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekAnchor
    ) {
        ReportCalendarInfo info = reportCalendarQueryService.getCalendar(
                new GetReportCalendarCommand(userId, yearMonth, weekAnchor));
        return ApiResponse.success(ReportCalendarResponse.from(info));
    }


    @Operation(
            summary = "카테고리 분포도 조회",
            description = "오늘 기준 최근 1개월 소비기록의 카테고리별 비율을 내려준다. "
    )
    @GetMapping("/category-distribution")
    public ApiResponse<CategoryDistributionResponse> getCategoryDistribution(@ChapChapUserId Long userId) {
        CategoryDistributionInfo info = reportCategoryDistributionQueryService.getDistribution(userId);
        return ApiResponse.success(CategoryDistributionResponse.from(info));
    }

    @Operation(
            summary = "이번달 쌓인 기록 스티커보드 조회",
            description = "연월(yyyy-MM) 기준 날짜별 스티커/사진 비주얼을 반환한다. 기록 없는 날짜는 응답에서 제외된다."
    )
    @GetMapping("/sticker-board")
    public ApiResponse<StickerBoardResponse> getStickerBoard(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회할 연월, 예: 2026-08") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        StickerBoardInfo info = reportStickerBoardQueryService.getStickerBoard(userId, yearMonth);
        return ApiResponse.success(StickerBoardResponse.from(info));
    }

    @Operation(
            summary = "상단 핵심요약 카드 조회",
            description = "방문 기록에서 발견된 특징 1개를 반환한다. 하루 동안은 동일 카드를 유지하고, "
                    + "다음날은 이전에 노출한 유형을 제외하고 순환 노출한다. 기록이 부족하면 기본 안내 문구를 반환한다."
    )
    @GetMapping("/summary-highlight")
    public ApiResponse<SummaryHighlightResponse> getSummaryHighlight(@ChapChapUserId Long userId) {
        SummaryHighlightInfo info = reportSummaryQueryService.getSummaryHighlight(userId);
        return ApiResponse.success(SummaryHighlightResponse.from(info));
    }
}
