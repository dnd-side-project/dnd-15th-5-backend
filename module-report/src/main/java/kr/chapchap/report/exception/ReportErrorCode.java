package kr.chapchap.report.exception;

import kr.chapchap.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "해당 연월의 리포트를 찾을 수 없습니다."),
    MONTHLY_REPORT_AGGREGATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "R002", "월간 리포트 집계에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
