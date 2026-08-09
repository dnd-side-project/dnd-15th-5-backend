package kr.chapchap.report.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.chapchap.core.web.response.ApiResponse;
import kr.chapchap.report.api.response.CurrentStatusResponse;
import kr.chapchap.report.application.command.CurrentStatusCommand;
import kr.chapchap.report.application.info.CurrentStatusInfo;
import kr.chapchap.report.application.service.ReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "Report", description = "리포트 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportQueryService reportQueryService;

    @Operation(
            summary = "현재 리포트 현황 조회",
            description = "연월(yyyy-MM) 기준 이번 달 누적 소비건수와,주별 소비건수 및 멘트 노출 "
    )
    @GetMapping("/{yearMonth}")
    public ApiResponse<CurrentStatusResponse> getCurrentStatus(
            @Parameter(description = "조회할 연월, 예: 2026-08") @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @Parameter(description = "유저 ID") @RequestParam Long userId
    ) {
        CurrentStatusInfo info = reportQueryService.getCurrentStatus(new CurrentStatusCommand(userId, yearMonth));
        return ApiResponse.success(CurrentStatusResponse.from(info));
    }
}
