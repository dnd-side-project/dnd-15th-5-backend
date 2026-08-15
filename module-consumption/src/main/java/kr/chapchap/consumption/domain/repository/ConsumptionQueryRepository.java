package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.CategoryCountRow;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceFirstStickerRow;
import kr.chapchap.consumption.domain.entity.PlacePopularityRow;
import kr.chapchap.consumption.domain.entity.PlaceStickerRow;
import kr.chapchap.consumption.domain.entity.PlaceVisitStatsRow;
import kr.chapchap.consumption.domain.entity.StickerCountRow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ConsumptionQueryRepository {

    List<Consumption> searchByCursor(Long userId, LocalDate monthStart, LocalDate monthEndExclusive,
                                      LocalDate cursorPurchaseDate, LocalTime cursorPurchaseTime, Long cursorId,
                                      int fetchSize);

    List<Consumption> findAllByUserAndDateRange(Long userId, LocalDate from, LocalDate toExclusive);

    long countDistinctPlacesByUserAndDateRange(Long userId, LocalDate from, LocalDate toExclusive);

    List<Long> findDistinctUserIdsByDateRange(LocalDate from, LocalDate toExclusive);

    List<PlaceCategoryVisitRow> aggregateVisitedPlacesByCategory(Long userId, List<String> categories);

    List<PlaceFirstStickerRow> findFirstStickerItemIdsByPlace(Long userId, List<Long> placeIds);

    //  module-recommendation의 가게 추천 좁혀진 placeIds만 대상
    List<PlacePopularityRow> aggregatePopularityByPlaceIds(List<Long> placeIds);

    List<CategoryCountRow> aggregateCategoryCounts(Long userId, LocalDate from, LocalDate toExclusive);

    List<PlaceCategoryVisitRow> aggregatePlaceRankingByCursor(Long userId, LocalDate from, LocalDate toExclusive,
                                                                List<String> categories, Long cursorVisitCount,
                                                                Long cursorPlaceId, int fetchSize);

    // 장소 상세
    Optional<PlaceVisitStatsRow> findVisitStats(Long userId, Long placeId);

    long countVisits(Long userId, Long placeId, LocalDate from, LocalDate toExclusive);

    List<PlaceStickerRow> findRecentStickersByPlace(Long userId, Long placeId, int limit);

    List<StickerCountRow> aggregateStickerCountsByPlace(Long userId, Long placeId);

    List<Consumption> searchPlaceVisitsByCursor(Long userId, Long placeId, LocalDate cursorPurchaseDate,
                                                 LocalTime cursorPurchaseTime, Long cursorId, int fetchSize);
}
