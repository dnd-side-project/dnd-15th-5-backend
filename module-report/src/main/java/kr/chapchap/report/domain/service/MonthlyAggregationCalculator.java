package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.MonthlyAggregationResult;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedCategoryStat;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedPlaceRank;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedTimePattern;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedTownRank;
import kr.chapchap.report.domain.entity.MonthlyVisitActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class MonthlyAggregationCalculator {

    private static final int TOWN_RANK_LIMIT = 3;
    private static final int PLACE_RANK_LIMIT = 3;
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public MonthlyAggregationResult calculate(
            List<MonthlyVisitActivity> monthActivities,
            Set<Long> priorVisitedPlaceIds,
            Set<String> priorVisitedTownNames,
            Map<Long, LocalDate> earliestVisitDateByPlaceId
    ) {
        int totalVisitCount = monthActivities.size();

        return new MonthlyAggregationResult(
                totalVisitCount,
                calculateNewTownCount(monthActivities, priorVisitedTownNames),
                calculateNewPlaceCount(monthActivities, priorVisitedPlaceIds),
                calculateCategoryStats(monthActivities, totalVisitCount),
                calculateTownRanks(monthActivities),
                calculatePlaceRanks(monthActivities, earliestVisitDateByPlaceId),
                calculateTimePatterns(monthActivities)
        );
    }

    private List<AggregatedCategoryStat> calculateCategoryStats(List<MonthlyVisitActivity> activities, int totalVisitCount) {
        if (totalVisitCount == 0) {
            return List.of();
        }

        return activities.stream()
                .collect(Collectors.groupingBy(MonthlyVisitActivity::category, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new AggregatedCategoryStat(
                        entry.getKey(),
                        BigDecimal.valueOf(entry.getValue())
                                .multiply(HUNDRED)
                                .divide(BigDecimal.valueOf(totalVisitCount), PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(AggregatedCategoryStat::percentage).reversed()
                        .thenComparing(AggregatedCategoryStat::category))
                .toList();
    }

    private List<AggregatedTownRank> calculateTownRanks(List<MonthlyVisitActivity> activities) {
        Map<String, Long> visitCountByTown = activities.stream()
                .map(MonthlyVisitActivity::dongName)
                .filter(dongName -> dongName != null && !dongName.isBlank())
                .collect(Collectors.groupingBy(dongName -> dongName, Collectors.counting()));

        List<Map.Entry<String, Long>> ranked = visitCountByTown.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOWN_RANK_LIMIT)
                .toList();

        List<AggregatedTownRank> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Long> entry : ranked) {
            result.add(new AggregatedTownRank(rank++, entry.getKey(), entry.getValue().intValue()));
        }
        return result;
    }

    private List<AggregatedPlaceRank> calculatePlaceRanks(List<MonthlyVisitActivity> activities,
                                                            Map<Long, LocalDate> earliestVisitDateByPlaceId) {
        record PlaceAggregate(Long placeId, String placeName, long visitCount) {
        }

        Map<Long, List<MonthlyVisitActivity>> byPlace = activities.stream()
                .collect(Collectors.groupingBy(MonthlyVisitActivity::placeId));

        List<PlaceAggregate> ranked = byPlace.entrySet().stream()
                .map(entry -> new PlaceAggregate(
                        entry.getKey(),
                        entry.getValue().get(0).placeName(),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparingLong(PlaceAggregate::visitCount).reversed()
                        .thenComparing(PlaceAggregate::placeId))
                .limit(PLACE_RANK_LIMIT)
                .toList();

        List<AggregatedPlaceRank> result = new ArrayList<>();
        int rank = 1;
        for (PlaceAggregate aggregate : ranked) {
            result.add(new AggregatedPlaceRank(
                    rank++,
                    aggregate.placeId(),
                    aggregate.placeName(),
                    (int) aggregate.visitCount(),
                    earliestVisitDateByPlaceId.get(aggregate.placeId())
            ));
        }
        return result;
    }

    private List<AggregatedTimePattern> calculateTimePatterns(List<MonthlyVisitActivity> activities) {
        record TimeKey(int dayOfWeek, int visitHour) {
        }

        Map<TimeKey, Long> counts = activities.stream()
                .filter(activity -> activity.purchaseTime() != null)
                .collect(Collectors.groupingBy(
                        activity -> new TimeKey(activity.purchaseDate().getDayOfWeek().getValue(), activity.purchaseTime().getHour()),
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .map(entry -> new AggregatedTimePattern(entry.getKey().dayOfWeek(), entry.getKey().visitHour(), entry.getValue().intValue()))
                .sorted(Comparator.comparingInt(AggregatedTimePattern::dayOfWeek)
                        .thenComparingInt(AggregatedTimePattern::visitHour))
                .toList();
    }

    private int calculateNewTownCount(List<MonthlyVisitActivity> activities, Set<String> priorVisitedTownNames) {
        return (int) activities.stream()
                .map(MonthlyVisitActivity::dongName)
                .filter(dongName -> dongName != null && !dongName.isBlank())
                .distinct()
                .filter(dongName -> !priorVisitedTownNames.contains(dongName))
                .count();
    }

    private int calculateNewPlaceCount(List<MonthlyVisitActivity> activities, Set<Long> priorVisitedPlaceIds) {
        return (int) activities.stream()
                .map(MonthlyVisitActivity::placeId)
                .distinct()
                .filter(placeId -> !priorVisitedPlaceIds.contains(placeId))
                .count();
    }

}
