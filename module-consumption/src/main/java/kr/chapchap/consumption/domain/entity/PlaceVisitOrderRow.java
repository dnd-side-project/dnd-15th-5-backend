package kr.chapchap.consumption.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;

// 가게별 방문 순번(N회째 방문) 계산용 - 소비기록 가벼운 투영
public record PlaceVisitOrderRow(Long consumptionId, Long placeId, LocalDate purchaseDate, LocalTime purchaseTime) {
}
