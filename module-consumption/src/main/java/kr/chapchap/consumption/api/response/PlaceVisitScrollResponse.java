package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo;
import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo.VisitInfo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PlaceVisitScrollResponse(
        List<PlaceVisitResponse> visits,
        boolean hasNext,
        LocalDate nextCursorPurchaseDate,
        LocalTime nextCursorPurchaseTime,
        Long nextCursorId
) {
    public static PlaceVisitScrollResponse from(PlaceVisitScrollInfo info) {
        List<PlaceVisitResponse> visits = info.visits().stream().map(PlaceVisitResponse::from).toList();
        return new PlaceVisitScrollResponse(
                visits, info.hasNext(), info.nextCursorPurchaseDate(), info.nextCursorPurchaseTime(),
                info.nextCursorId()
        );
    }

    public record PlaceVisitResponse(LocalDate visitedAt, Long amount) {
        public static PlaceVisitResponse from(VisitInfo info) {
            return new PlaceVisitResponse(info.visitedAt(), info.amount());
        }
    }
}
