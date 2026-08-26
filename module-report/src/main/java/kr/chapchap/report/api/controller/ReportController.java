package kr.chapchap.report.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.report.api.response.CurrentStatusResponse;
import kr.chapchap.report.api.response.MonthlyReportResponse;
import kr.chapchap.report.api.response.PersonaCardResponse;
import kr.chapchap.report.api.response.ShareLinkResponse;
import kr.chapchap.report.application.command.CurrentStatusCommand;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.CurrentStatusInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.service.MonthlyReportQueryService;
import kr.chapchap.report.application.service.ReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(
            summary = "취향카드 공유 링크 발급",
            description = "연월(yyyy-MM) 기준 본인 리포트의 취향카드 공유 토큰을 발급한다. 이미 발급된 적이 있으면 기존 토큰을 그대로 반환한다."
    )
    @PostMapping("/monthly/share")
    public ApiResponse<ShareLinkResponse> issueShareLink(
            @ChapChapUserId Long userId,
            @Parameter(description = "조회할 연월, 예: 2026-07") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        String shareToken = monthlyReportQueryService.issueShareToken(new GetMonthlyReportCommand(userId, yearMonth));
        return ApiResponse.success(new ShareLinkResponse(shareToken));
    }

    @Operation(
            summary = "공유받은 취향카드 조회",
            description = "공유 토큰으로 페르소나 정보만 담긴 취향카드를 조회한다. 로그인 없이 접근 가능."
    )
    @SecurityRequirements
    @GetMapping("/share/{shareToken}")
    public ApiResponse<PersonaCardResponse> getSharedPersonaCard(@PathVariable String shareToken) {
        return ApiResponse.success(PersonaCardResponse.from(monthlyReportQueryService.getPersonaCard(shareToken)));
    }
}
