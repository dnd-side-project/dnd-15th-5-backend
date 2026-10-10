package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiptImageValidator {

    private final ImageValidator imageValidator;

    public String validateAndGetContentType(byte[] content) {
        try {
            return imageValidator.validateAndGetContentType(content);
        } catch (BusinessException exception) {
            ConsumptionErrorCode code = switch ((ConsumptionErrorCode) exception.getErrorCode()) {
                case IMAGE_SIZE_EXCEEDED -> ConsumptionErrorCode.RECEIPT_IMAGE_SIZE_EXCEEDED;
                case UNSUPPORTED_IMAGE_FORMAT -> ConsumptionErrorCode.UNSUPPORTED_RECEIPT_IMAGE_FORMAT;
                case IMAGE_DIMENSION_EXCEEDED -> ConsumptionErrorCode.RECEIPT_IMAGE_DIMENSION_EXCEEDED;
                default -> ConsumptionErrorCode.INVALID_RECEIPT_IMAGE;
            };
            throw new BusinessException(code, exception);
        }
    }
}
