package kr.chapchap.consumption.application.info;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ConsumptionScrollInfo(
        List<ConsumptionInfo> consumptions,
        boolean hasNext,
        LocalDate nextCursorPurchaseDate,
        LocalTime nextCursorPurchaseTime,
        Long nextCursorId
) {
}
