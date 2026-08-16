package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

public record ConsumptionSearchCommand(
        Long userId,
        YearMonth yearMonth,
        LocalDate cursorPurchaseDate,
        LocalTime cursorPurchaseTime,
        Long cursorId,
        int size
) {
    public ConsumptionSearchCommand {
        if (size < 1) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_SIZE);
        }
    }
}
