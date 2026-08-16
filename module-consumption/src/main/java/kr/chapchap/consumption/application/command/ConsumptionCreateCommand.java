package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionCreateCommand(
        Long userId,
        Long receiptImageId,
        PlaceResolveCommand place,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        Long amount,
        String category
) {

    private static final int MAX_CATEGORY_LENGTH = 40;

    public ConsumptionCreateCommand {
        category = category == null ? null : category.trim();

        if (userId == null || userId <= 0
                || (receiptImageId != null && receiptImageId <= 0)
                || place == null
                || purchaseDate == null
                || purchaseTime == null
                || amount == null || amount <= 0
                || category == null || category.isBlank() || category.length() > MAX_CATEGORY_LENGTH) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }
    }
}
