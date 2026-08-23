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

    List<Consumption> searchLatestVisitedPlacesByCursor(
            Long userId,
            LocalDate cursorPurchaseDate,
            LocalTime cursorPurchaseTime,
            Long cursorId,
            int fetchSize
    );

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

    // from/toExclusive가 둘 다 null이면 전체기간 (장소 상세용), 아니면 그 기간으로 좁힘 (월간 리포트 1위 장소용)
    List<PlaceStickerRow> findRecentStickersByPlace(Long userId, Long placeId, LocalDate from, LocalDate toExclusive, int limit);

    List<StickerCountRow> aggregateStickerCountsByPlace(Long userId, Long placeId);

    List<Consumption> searchPlaceVisitsByCursor(Long userId, Long placeId, LocalDate cursorPurchaseDate,
                                                 LocalTime cursorPurchaseTime, Long cursorId, int fetchSize);

    List<Long> findRecentStickerItemIdsByUser(Long userId, LocalDate from, LocalDate toExclusive);
}
