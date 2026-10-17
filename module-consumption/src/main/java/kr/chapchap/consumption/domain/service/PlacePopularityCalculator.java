package kr.chapchap.consumption.domain.service;

import kr.chapchap.consumption.domain.entity.PlacePopularityRow;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class PlacePopularityCalculator {

    private static final Comparator<PlacePopularityRow> CATEGORY_ORDER =
            Comparator.comparingLong(PlacePopularityRow::visitCount).reversed()
                    .thenComparing(PlacePopularityRow::category);

    private PlacePopularityCalculator() {
    }

    public static List<PlacePopularityRow> aggregate(List<PlacePopularityRow> categoryCounts) {
        return categoryCounts.stream()
                .collect(Collectors.groupingBy(PlacePopularityRow::placeId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    List<PlacePopularityRow> rows = entry.getValue();
                    String category = rows.stream().min(CATEGORY_ORDER).orElseThrow().category();
                    long visitCount = rows.stream().mapToLong(PlacePopularityRow::visitCount).sum();
                    LocalDate lastVisitedDate = rows.stream().map(PlacePopularityRow::lastVisitedDate)
                            .max(Comparator.naturalOrder()).orElseThrow();
                    return new PlacePopularityRow(entry.getKey(), category, visitCount, lastVisitedDate);
                })
                .toList();
    }
}
