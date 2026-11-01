package kr.chapchap.consumption.application.info;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PlaceVisitScrollInfo(
        List<VisitInfo> visits,
        boolean hasNext,
        LocalDate nextCursorPurchaseDate,
        LocalTime nextCursorPurchaseTime,
        Long nextCursorId,
        String thumbnailUrl
) {

    public record VisitInfo(Long consumptionId, LocalDate visitedAt, Long amount) {
    }
}
