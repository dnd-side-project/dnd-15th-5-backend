package kr.chapchap.consumption.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.chapchap.core.persistence.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "consumptions")
public class Consumption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "purchase_time")
    private LocalTime purchaseTime;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Builder
    private Consumption(LocalDate purchaseDate, LocalTime purchaseTime, Long amount, String category, Long userId, Long placeId) {
        this.purchaseDate = purchaseDate;
        this.purchaseTime = purchaseTime;
        this.amount = amount;
        this.category = category;
        this.userId = userId;
        this.placeId = placeId;
    }
}
