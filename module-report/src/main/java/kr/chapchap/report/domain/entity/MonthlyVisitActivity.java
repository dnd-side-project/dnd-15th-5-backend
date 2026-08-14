package kr.chapchap.report.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;

// 월간 리포트 집계 배치가 사용하는, 동네/가게 이름까지 해석된 소비 활동 1건
public record MonthlyVisitActivity(
        Long placeId,
        String dongName,
        String placeName,
        String category,
        LocalDate purchaseDate,
        LocalTime purchaseTime
) {
}
