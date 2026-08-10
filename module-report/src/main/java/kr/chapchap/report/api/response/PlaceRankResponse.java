package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.PlaceRankInfo;

import java.time.format.DateTimeFormatter;

public record PlaceRankResponse(
        int rank,
        String placeName,
        int visitCount,
        String firstVisitedDate
) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static PlaceRankResponse from(PlaceRankInfo info) {
        String firstVisitedDate = info.firstVisitedDate() != null
                ? DATE_FORMAT.format(info.firstVisitedDate())
                : null;
        return new PlaceRankResponse(info.rank(), info.placeName(), info.visitCount(), firstVisitedDate);
    }
}
