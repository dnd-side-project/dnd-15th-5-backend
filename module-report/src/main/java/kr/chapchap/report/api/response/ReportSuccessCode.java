package kr.chapchap.report.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportSuccessCode {

    NO_REPORT_FOR_MONTH(HttpStatus.OK, "RS001", "해당 연월의 리포트가 존재하지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
