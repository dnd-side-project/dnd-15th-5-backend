package kr.chapchap.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "C003", "필수 요청 파라미터가 누락되었습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),
    INVALID_AUTHENTICATION_CREDENTIALS(HttpStatus.UNAUTHORIZED, "C006", "유효하지 않은 인증 정보입니다."),
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "C007", "외부 서비스 연동에 실패했습니다."),
    UPLOAD_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "C008", "업로드 가능한 파일 크기를 초과했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
