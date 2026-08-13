package kr.chapchap.consumption.infra.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.QConsumption;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class ConsumptionQueryRepositoryImpl implements ConsumptionQueryRepository {


    private static final long MAX_VISITED_PLACE_ROWS = 2000;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Consumption> searchByCursor(Long userId, LocalDate monthStart, LocalDate monthEndExclusive,
                                             LocalDate cursorPurchaseDate, LocalTime cursorPurchaseTime,
                                             Long cursorId, int fetchSize) {
        QConsumption consumption = QConsumption.consumption;

        BooleanBuilder condition = new BooleanBuilder()
                .and(consumption.userId.eq(userId))
                .and(consumption.purchaseDate.goe(monthStart))
                .and(consumption.purchaseDate.lt(monthEndExclusive));

        if (cursorPurchaseDate != null && cursorId != null) {
            BooleanExpression sameDateTieBreak = cursorPurchaseTime == null
                    ? consumption.purchaseTime.isNull().and(consumption.id.lt(cursorId))
                    : consumption.purchaseTime.isNull()
                            .or(consumption.purchaseTime.lt(cursorPurchaseTime))
                            .or(consumption.purchaseTime.eq(cursorPurchaseTime).and(consumption.id.lt(cursorId)));

            condition.and(
                    consumption.purchaseDate.lt(cursorPurchaseDate)
                            .or(consumption.purchaseDate.eq(cursorPurchaseDate).and(sameDateTieBreak))
            );
        }

        return queryFactory
                .selectFrom(consumption)
                .where(condition)
                .orderBy(consumption.purchaseDate.desc(), consumption.purchaseTime.desc().nullsLast(),
                        consumption.id.desc())
                .limit(fetchSize)
                .fetch();
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
}
