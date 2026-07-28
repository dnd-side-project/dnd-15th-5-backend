package com.dnd.core.domain.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "C003", "필수 요청 파라미터가 누락되었습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}