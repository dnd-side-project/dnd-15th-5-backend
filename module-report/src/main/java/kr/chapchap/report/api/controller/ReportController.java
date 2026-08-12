package kr.chapchap.report.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.report.api.response.CurrentStatusResponse;
import kr.chapchap.report.api.response.MonthlyReportResponse;
import kr.chapchap.report.application.command.CurrentStatusCommand;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.CurrentStatusInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.service.MonthlyReportQueryService;
import kr.chapchap.report.application.service.ReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Report", description = "리포트 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportQueryService reportQueryService;
    private final MonthlyReportQueryService monthlyReportQueryService;

    @Operation(
            summary = "현재 리포트 현황 조회",
            description = "연월(yyyy-MM) 기준 이번 달 누적 소비건수와,주별 소비건수 및 멘트 노출 "
    )
    @GetMapping("/current")
    public ApiResponse<CurrentStatusResponse> getCurrentStatus(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회할 연월, 예: 2026-08") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        CurrentStatusInfo info = reportQueryService.getCurrentStatus(new CurrentStatusCommand(userId, yearMonth));
        return ApiResponse.success(CurrentStatusResponse.from(info));
    }

    @Operation(
            summary = "월간 리포트 조회",
            description = "연월(yyyy-MM) 기준으로 집계된 월간 리포트를 조회한다."
    )
    @GetMapping("/monthly")
    public ApiResponse<MonthlyReportResponse> getMonthlyReport(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회할 연월, 예: 2026-07") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        MonthlyReportInfo info = monthlyReportQueryService.getMonthlyReport(new GetMonthlyReportCommand(userId, yearMonth));
        return ApiResponse.success(MonthlyReportResponse.from(info));
    }
}
