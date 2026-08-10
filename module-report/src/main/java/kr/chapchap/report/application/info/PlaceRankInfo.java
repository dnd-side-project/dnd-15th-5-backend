package kr.chapchap.report.application.info;

import java.time.LocalDate;

public record PlaceRankInfo(
        int rank,
        String placeName,
        int visitCount,
        LocalDate firstVisitedDate
) {
}
