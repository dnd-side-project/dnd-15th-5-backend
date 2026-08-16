package kr.chapchap.place.exception;

import kr.chapchap.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {

    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE001", "장소의 위치 정보를 찾을 수 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE002", "존재하지 않는 장소입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
