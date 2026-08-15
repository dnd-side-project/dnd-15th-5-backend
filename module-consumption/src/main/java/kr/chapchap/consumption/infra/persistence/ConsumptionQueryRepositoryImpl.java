package kr.chapchap.consumption.infra.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kr.chapchap.consumption.domain.entity.CategoryCountRow;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceFirstStickerRow;
import kr.chapchap.consumption.domain.entity.PlacePopularityRow;
import kr.chapchap.consumption.domain.entity.PlaceStickerRow;
import kr.chapchap.consumption.domain.entity.PlaceVisitStatsRow;
import kr.chapchap.consumption.domain.entity.QConsumption;
import kr.chapchap.consumption.domain.entity.StickerCountRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ConsumptionQueryRepositoryImpl implements ConsumptionQueryRepository {


    private static final long MAX_VISITED_PLACE_ROWS = 2000;

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public List<Consumption> searchByCursor(Long userId, LocalDate monthStart, LocalDate monthEndExclusive,
                                             LocalDate cursorPurchaseDate, LocalTime cursorPurchaseTime,
                                             Long cursorId, int fetchSize) {
        QConsumption consumption = QConsumption.consumption;

        BooleanBuilder condition = new BooleanBuilder()
                .and(consumption.userId.eq(userId))
                .and(consumption.purchaseDate.goe(monthStart))
                .and(consumption.purchaseDate.lt(monthEndExclusive));

        BooleanExpression dateCursorCondition = dateCursorCondition(consumption, cursorPurchaseDate, cursorPurchaseTime, cursorId);
        if (dateCursorCondition != null) {
            condition.and(dateCursorCondition);
        }

        return queryFactory
                .selectFrom(consumption)
                .where(condition)
                .orderBy(consumption.purchaseDate.desc(), consumption.purchaseTime.desc().nullsLast(),
                        consumption.id.desc())
                .limit(fetchSize)
                .fetch();
    }

    // purchaseDate/purchaseTime/id 커서 무한스크롤 공통 타이브레이크 조건. 첫 페이지(커서 없음)는 null.
    private BooleanExpression dateCursorCondition(QConsumption consumption, LocalDate cursorPurchaseDate,
                                                   LocalTime cursorPurchaseTime, Long cursorId) {
        if (cursorPurchaseDate == null || cursorId == null) {
            return null;
        }

        BooleanExpression sameDateTieBreak = cursorPurchaseTime == null
                ? consumption.purchaseTime.isNull().and(consumption.id.lt(cursorId))
                : consumption.purchaseTime.isNull()
                        .or(consumption.purchaseTime.lt(cursorPurchaseTime))
                        .or(consumption.purchaseTime.eq(cursorPurchaseTime).and(consumption.id.lt(cursorId)));

        return consumption.purchaseDate.lt(cursorPurchaseDate)
                .or(consumption.purchaseDate.eq(cursorPurchaseDate).and(sameDateTieBreak));
    }

    @Override
    public long countDistinctPlacesByUserAndDateRange(Long userId, LocalDate from, LocalDate toExclusive) {
        QConsumption consumption = QConsumption.consumption;

        Long count = queryFactory
                .select(consumption.placeId.countDistinct())
                .from(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.purchaseDate.goe(from),
                        consumption.purchaseDate.lt(toExclusive)
                )
                .fetchOne();
        return count == null ? 0 : count;
    }

    @Override
    public List<Consumption> findAllByUserAndDateRange(Long userId, LocalDate from, LocalDate toExclusive) {
        QConsumption consumption = QConsumption.consumption;

        return queryFactory
                .selectFrom(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.purchaseDate.goe(from),
                        consumption.purchaseDate.lt(toExclusive)
                )
                .fetch();
    }

    @Override
    public List<Long> findDistinctUserIdsByDateRange(LocalDate from, LocalDate toExclusive) {
        QConsumption consumption = QConsumption.consumption;

        return queryFactory
                .select(consumption.userId)
                .distinct()
                .from(consumption)
                .where(
                        consumption.purchaseDate.goe(from),
                        consumption.purchaseDate.lt(toExclusive)
                )
                .fetch();
    }

    @Override
    public List<PlaceCategoryVisitRow> aggregateVisitedPlacesByCategory(Long userId, List<String> categories) {
        QConsumption consumption = QConsumption.consumption;

        BooleanBuilder condition = new BooleanBuilder()
                .and(consumption.userId.eq(userId));
        if (categories != null && !categories.isEmpty()) {
            condition.and(consumption.category.in(categories));
        }

        return queryFactory
                .select(Projections.constructor(PlaceCategoryVisitRow.class,
                        consumption.placeId,
                        consumption.category.max(),
                        consumption.count()))
                .from(consumption)
                .where(condition)
                .groupBy(consumption.placeId)
                .orderBy(consumption.count().desc())
                .limit(MAX_VISITED_PLACE_ROWS)
                .fetch();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PlaceFirstStickerRow> findFirstStickerItemIdsByPlace(Long userId, List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }

        Query query = entityManager.createNativeQuery(
                "SELECT DISTINCT ON (place_id) place_id, sticker_item_id "
                        + "FROM consumptions "
                        + "WHERE user_id = :userId AND place_id IN (:placeIds) AND sticker_item_id IS NOT NULL "
                        + "ORDER BY place_id, purchase_date, purchase_time NULLS LAST, id"
        );
        query.setParameter("userId", userId);
        query.setParameter("placeIds", placeIds);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new PlaceFirstStickerRow(((Number) row[0]).longValue(), ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    public List<PlacePopularityRow> aggregatePopularityByPlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }

        QConsumption consumption = QConsumption.consumption;

        return queryFactory
                .select(Projections.constructor(PlacePopularityRow.class,
                        consumption.placeId,
                        consumption.category,
                        consumption.count(),
                        consumption.purchaseDate.max()))
                .from(consumption)
                .where(consumption.placeId.in(placeIds))
                .groupBy(consumption.placeId, consumption.category)
                .fetch();
    }

    @Override
    public List<CategoryCountRow> aggregateCategoryCounts(Long userId, LocalDate from, LocalDate toExclusive) {
        QConsumption consumption = QConsumption.consumption;

        return queryFactory
                .select(Projections.constructor(CategoryCountRow.class,
                        consumption.category,
                        consumption.count()))
                .from(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.purchaseDate.goe(from),
                        consumption.purchaseDate.lt(toExclusive)
                )
                .groupBy(consumption.category)
                .fetch();
    }

    @Override
    public List<PlaceCategoryVisitRow> aggregatePlaceRankingByCursor(Long userId, LocalDate from, LocalDate toExclusive,
                                                                       List<String> categories, Long cursorVisitCount,
                                                                       Long cursorPlaceId, int fetchSize) {
        QConsumption consumption = QConsumption.consumption;

        BooleanBuilder condition = new BooleanBuilder()
                .and(consumption.userId.eq(userId));

        if (categories != null && !categories.isEmpty()) {
            condition.and(consumption.category.in(categories));
        }
        if (from != null && toExclusive != null) {
            condition.and(consumption.purchaseDate.goe(from))
                    .and(consumption.purchaseDate.lt(toExclusive));
        }

        BooleanBuilder having = new BooleanBuilder();
        if (cursorVisitCount != null && cursorPlaceId != null) {
            having.and(
                    consumption.count().lt(cursorVisitCount)
                            .or(consumption.count().eq(cursorVisitCount).and(consumption.placeId.lt(cursorPlaceId)))
            );
        }

        return queryFactory
                .select(Projections.constructor(PlaceCategoryVisitRow.class,
                        consumption.placeId,
                        consumption.category.max(),
                        consumption.count()))
                .from(consumption)
                .where(condition)
                .groupBy(consumption.placeId)
                .having(having)
                .orderBy(consumption.count().desc(), consumption.placeId.desc())
                .limit(fetchSize)
                .fetch();
    }

    @Override
    public Optional<PlaceVisitStatsRow> findVisitStats(Long userId, Long placeId) {
        QConsumption consumption = QConsumption.consumption;

        PlaceVisitStatsRow row = queryFactory
                .select(Projections.constructor(PlaceVisitStatsRow.class,
                        consumption.category.max(),
                        consumption.count(),
                        consumption.purchaseDate.min()))
                .from(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.placeId.eq(placeId)
                )
                .fetchOne();

        return Optional.ofNullable(row).filter(r -> r.totalVisitCount() != null && r.totalVisitCount() > 0);
    }

    @Override
    public long countVisits(Long userId, Long placeId, LocalDate from, LocalDate toExclusive) {
        QConsumption consumption = QConsumption.consumption;

        Long count = queryFactory
                .select(consumption.count())
                .from(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.placeId.eq(placeId),
                        consumption.purchaseDate.goe(from),
                        consumption.purchaseDate.lt(toExclusive)
                )
                .fetchOne();
        return count == null ? 0 : count;
    }

    @Override
    public List<PlaceStickerRow> findRecentStickersByPlace(Long userId, Long placeId, int limit) {
        QConsumption consumption = QConsumption.consumption;

        return queryFactory
                .select(Projections.constructor(PlaceStickerRow.class,
                        consumption.stickerItemId,
                        consumption.purchaseDate))
                .from(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.placeId.eq(placeId),
                        consumption.stickerItemId.isNotNull()
                )
                .orderBy(consumption.purchaseDate.desc(), consumption.purchaseTime.desc().nullsLast(),
                        consumption.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<StickerCountRow> aggregateStickerCountsByPlace(Long userId, Long placeId) {
        QConsumption consumption = QConsumption.consumption;

        return queryFactory
                .select(Projections.constructor(StickerCountRow.class,
                        consumption.stickerItemId,
                        consumption.count()))
                .from(consumption)
                .where(
                        consumption.userId.eq(userId),
                        consumption.placeId.eq(placeId),
                        consumption.stickerItemId.isNotNull()
                )
                .groupBy(consumption.stickerItemId)
                .orderBy(consumption.count().desc())
                .fetch();
    }

    @Override
    public List<Consumption> searchPlaceVisitsByCursor(Long userId, Long placeId, LocalDate cursorPurchaseDate,
                                                         LocalTime cursorPurchaseTime, Long cursorId, int fetchSize) {
        QConsumption consumption = QConsumption.consumption;

        BooleanBuilder condition = new BooleanBuilder()
                .and(consumption.userId.eq(userId))
                .and(consumption.placeId.eq(placeId));

        BooleanExpression dateCursorCondition = dateCursorCondition(consumption, cursorPurchaseDate, cursorPurchaseTime, cursorId);
        if (dateCursorCondition != null) {
            condition.and(dateCursorCondition);
        }

        return queryFactory
                .selectFrom(consumption)
                .where(condition)
                .orderBy(consumption.purchaseDate.desc(), consumption.purchaseTime.desc().nullsLast(),
                        consumption.id.desc())
                .limit(fetchSize)
                .fetch();
    }
}
