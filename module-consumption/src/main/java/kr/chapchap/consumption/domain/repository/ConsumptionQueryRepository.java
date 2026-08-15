package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceFirstStickerRow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ConsumptionQueryRepository {

    List<Consumption> searchByCursor(Long userId, LocalDate monthStart, LocalDate monthEndExclusive,
                                      LocalDate cursorPurchaseDate, LocalTime cursorPurchaseTime, Long cursorId,
                                      int fetchSize);

    List<Consumption> findAllByUserAndDateRange(Long userId, LocalDate from, LocalDate toExclusive);

    long countDistinctPlacesByUserAndDateRange(Long userId, LocalDate from, LocalDate toExclusive);

    List<Long> findDistinctUserIdsByDateRange(LocalDate from, LocalDate toExclusive);

    List<PlaceCategoryVisitRow> aggregateVisitedPlacesByCategory(Long userId, List<String> categories);

    List<PlaceFirstStickerRow> findFirstStickerItemIdsByPlace(Long userId, List<Long> placeIds);
}
