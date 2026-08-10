package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.ConsumptionScrollInfo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ConsumptionScrollResponse(
        List<ConsumptionResponse> consumptions,
        boolean hasNext,
        LocalDate nextCursorPurchaseDate,
        LocalTime nextCursorPurchaseTime,
        Long nextCursorId
) {
    public static ConsumptionScrollResponse from(ConsumptionScrollInfo info) {
        List<ConsumptionResponse> consumptions = info.consumptions().stream()
                .map(ConsumptionResponse::from)
                .toList();
        return new ConsumptionScrollResponse(
                consumptions, info.hasNext(), info.nextCursorPurchaseDate(), info.nextCursorPurchaseTime(),
                info.nextCursorId()
        );
    }
}
