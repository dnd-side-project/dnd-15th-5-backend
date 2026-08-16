package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;

public record PlaceVisitSearchCommand(
        Long userId,
        Long placeId,
        LocalDate cursorPurchaseDate,
        LocalTime cursorPurchaseTime,
        Long cursorId,
        int size
) {
    public PlaceVisitSearchCommand {
        if (size < 1) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_SIZE);
        }
    }
}
