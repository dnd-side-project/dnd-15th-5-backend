package kr.chapchap.consumption.application.command;

import java.time.LocalDate;
import java.time.LocalTime;

public record PlaceVisitSearchCommand(
        Long userId,
        Long placeId,
        LocalDate cursorPurchaseDate,
        LocalTime cursorPurchaseTime,
        Long cursorId,
        int size
) { }
