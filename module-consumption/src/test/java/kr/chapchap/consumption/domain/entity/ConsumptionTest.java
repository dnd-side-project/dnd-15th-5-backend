package kr.chapchap.consumption.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumptionTest {

    @Test
    void 필수_값으로_소비_기록을_생성한다() {
        // given
        LocalDate purchaseDate = LocalDate.of(2026, 8, 16);
        LocalTime purchaseTime = LocalTime.of(12, 30);

        // when
        Consumption consumption = Consumption.create(
                1L,
                101L,
                purchaseDate,
                purchaseTime,
                12_000L,
                "카페",
                7L
        );

        // then
        assertThat(consumption.getUserId()).isEqualTo(1L);
        assertThat(consumption.getPlaceId()).isEqualTo(101L);
        assertThat(consumption.getPurchaseDate()).isEqualTo(purchaseDate);
        assertThat(consumption.getPurchaseTime()).isEqualTo(purchaseTime);
        assertThat(consumption.getAmount()).isEqualTo(12_000L);
        assertThat(consumption.getCategory()).isEqualTo("카페");
        assertThat(consumption.getStickerItemId()).isEqualTo(7L);
    }

    @Test
    void 소비_금액이_0_이하면_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> Consumption.create(
                1L,
                101L,
                LocalDate.of(2026, 8, 16),
                LocalTime.of(12, 30),
                0L,
                "카페",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 구매_시간이_없으면_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> Consumption.create(
                1L,
                101L,
                LocalDate.of(2026, 8, 16),
                null,
                12_000L,
                "카페",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
