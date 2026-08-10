package kr.chapchap.consumption.application.command;

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
) { }
