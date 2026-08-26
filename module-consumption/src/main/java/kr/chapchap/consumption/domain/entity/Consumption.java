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

    @Column(name = "purchase_time", nullable = false)
    private LocalTime purchaseTime;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;


    @Column(name = "sticker_item_id", nullable = false)
    private Long stickerItemId;

    @Builder
    private Consumption(LocalDate purchaseDate, LocalTime purchaseTime, Long amount, String category, Long userId,
                         Long placeId, Long stickerItemId) {
        this.purchaseDate = purchaseDate;
        this.purchaseTime = purchaseTime;
        this.amount = amount;
        this.category = category;
        this.userId = userId;
        this.placeId = placeId;
        this.stickerItemId = stickerItemId;
    }

    public static Consumption create(
            Long userId,
            Long placeId,
            LocalDate purchaseDate,
            LocalTime purchaseTime,
            Long amount,
            String category,
            Long stickerItemId
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 식별자는 0보다 커야 합니다.");
        }
        if (placeId == null || placeId <= 0) {
            throw new IllegalArgumentException("장소 식별자는 0보다 커야 합니다.");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("구매 날짜는 필수입니다.");
        }
        if (purchaseTime == null) {
            throw new IllegalArgumentException("구매 시간은 필수입니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("소비 금액은 0보다 커야 합니다.");
        }
        if (category == null || category.isBlank() || category.length() > 40) {
            throw new IllegalArgumentException("카테고리는 1자 이상 40자 이하여야 합니다.");
        }
        if (stickerItemId == null || stickerItemId <= 0) {
            throw new IllegalArgumentException("스티커 식별자는 필수이며 0보다 커야 합니다.");
        }

        return new Consumption(
                purchaseDate,
                purchaseTime,
                amount,
                category,
                userId,
                placeId,
                stickerItemId
        );
    }
}
