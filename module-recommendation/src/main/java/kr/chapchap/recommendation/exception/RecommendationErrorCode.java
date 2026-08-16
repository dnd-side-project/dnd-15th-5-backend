package kr.chapchap.recommendation.exception;

import kr.chapchap.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "RECOMMENDATION001", "위도는 -90~90, 경도는 -180~180 범위여야 합니다."),
    INVALID_RADIUS(HttpStatus.BAD_REQUEST, "RECOMMENDATION002", "검색 반경은 0보다 크고 50km 이하여야 합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
