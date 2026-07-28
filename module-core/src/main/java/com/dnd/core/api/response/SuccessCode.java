package com.dnd.core.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode {

    OK(HttpStatus.OK, "S001", "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "S002", "리소스가 생성되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;


}