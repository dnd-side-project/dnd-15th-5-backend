package kr.chapchap.report.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.auth.ChapChapUserId;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.report.api.response.ReportCalendarResponse;
import kr.chapchap.report.application.command.GetReportCalendarCommand;
import kr.chapchap.report.application.info.ReportCalendarInfo;
import kr.chapchap.report.application.service.ReportCalendarQueryService;
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
@RequestMapping("/v1/reports")
public class ReportCalendarController {

    private final ReportCalendarQueryService reportCalendarQueryService;

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
}
