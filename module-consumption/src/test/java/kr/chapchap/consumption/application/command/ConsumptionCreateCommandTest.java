package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumptionCreateCommandTest {

    private static final PlaceResolveCommand PLACE = new PlaceResolveCommand(
            "google-place-id",
            "찹찹카페",
            "서울특별시 강남구 테헤란로 123",
            37.501,
            127.039
    );

    @Test
    void 필수_입력으로_소비_등록_명령을_생성한다() {
        // when
        ConsumptionCreateCommand command = new ConsumptionCreateCommand(
                1L,
                null,
                PLACE,
                LocalDate.of(2026, 8, 16),
                LocalTime.of(12, 30),
                12_000L,
                "카페"
        );

        // then
        assertThat(command.userId()).isEqualTo(1L);
        assertThat(command.receiptImageId()).isNull();
        assertThat(command.place()).isEqualTo(PLACE);
    }

    @Test
    void 금액이_0_이하면_유효하지_않은_입력_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> new ConsumptionCreateCommand(
                1L,
                null,
                PLACE,
                LocalDate.of(2026, 8, 16),
                LocalTime.of(12, 30),
                0L,
                "카페"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT)
        );
    }

    @Test
    void 구매_시간이_없으면_유효하지_않은_입력_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> new ConsumptionCreateCommand(
                1L,
                null,
                PLACE,
                LocalDate.of(2026, 8, 16),
                null,
                12_000L,
                "카페"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT)
        );
    }
}
