package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

public record ConsumptionImageUploadCommand(Long userId, byte[] content) {
    public ConsumptionImageUploadCommand {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }
    }
}
