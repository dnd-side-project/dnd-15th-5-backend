package kr.chapchap.consumption.exception;

import kr.chapchap.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsumptionErrorCode implements ErrorCode {
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
