package kr.chapchap.receipt.infra.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.chapchap.receipt.domain.entity.Consumption;
import kr.chapchap.receipt.domain.entity.QConsumption;
import kr.chapchap.receipt.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class ConsumptionQueryRepositoryImpl implements ConsumptionQueryRepository {

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
}
