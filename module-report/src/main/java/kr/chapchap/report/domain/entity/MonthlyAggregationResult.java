package kr.chapchap.report.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// MonthlyAggregationCalculator의 순수 계산 결과. 배치 서비스가 이 값을 Report/자식 엔티티로 변환해 저장한다.
public record MonthlyAggregationResult(
        int totalVisitCount,
        int newTownCount,
        int newPlaceCount,
        List<AggregatedCategoryStat> categoryStats,
        List<AggregatedTownRank> townRanks,
        List<AggregatedPlaceRank> placeRanks,
        List<AggregatedTimePattern> timePatterns
) {

    public record AggregatedCategoryStat(String category, BigDecimal percentage) {
    }

    public record AggregatedTownRank(int rank, String townName, int visitCount) {
    }

    public record AggregatedPlaceRank(int rank, Long placeId, String placeName, int visitCount, LocalDate firstVisitedDate) {
    }

    public record AggregatedTimePattern(int dayOfWeek, int visitHour, int visitCount) {
    }
}
