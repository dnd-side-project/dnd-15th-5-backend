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
    INVALID_SIZE(HttpStatus.BAD_REQUEST, "CONSUMPTION003", "size는 1 이상이어야 합니다."),
    INVALID_RECEIPT_IMAGE(HttpStatus.BAD_REQUEST, "CONSUMPTION004", "올바른 영수증 이미지 파일이 아닙니다."),
    RECEIPT_IMAGE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "CONSUMPTION005", "영수증 이미지는 5MB 이하여야 합니다."),
    UNSUPPORTED_RECEIPT_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "CONSUMPTION006", "영수증 이미지는 JPEG 또는 PNG 형식만 지원합니다."),
    RECEIPT_IMAGE_DIMENSION_EXCEEDED(HttpStatus.BAD_REQUEST, "CONSUMPTION007", "영수증 이미지 해상도는 4096x4096 이하여야 합니다."),
    RECEIPT_OCR_RECOGNITION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "CONSUMPTION008", "영수증을 인식할 수 없습니다."),
    RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CONSUMPTION009", "OCR 요청이 많습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
