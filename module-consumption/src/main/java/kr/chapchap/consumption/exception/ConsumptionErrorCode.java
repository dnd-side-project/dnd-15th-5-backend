package kr.chapchap.consumption.exception;

import kr.chapchap.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsumptionErrorCode implements ErrorCode {

    PLACE_VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONSUMPTION001", "해당 장소에 대한 방문 기록이 없습니다."),
    PLACE_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CONSUMPTION002", "장소의 위치 정보를 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
