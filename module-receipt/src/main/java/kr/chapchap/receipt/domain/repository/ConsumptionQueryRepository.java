package kr.chapchap.receipt.domain.repository;

import kr.chapchap.receipt.domain.entity.Consumption;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ConsumptionQueryRepository {

    List<Consumption> searchByCursor(Long userId, LocalDate monthStart, LocalDate monthEndExclusive,
                                      LocalDate cursorPurchaseDate, LocalTime cursorPurchaseTime, Long cursorId,
                                      int fetchSize);
}
