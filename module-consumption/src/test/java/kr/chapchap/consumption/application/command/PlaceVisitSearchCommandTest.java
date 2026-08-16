package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceVisitSearchCommandTest {

    @Test
    void size가_0이면_예외를_던진다() {
        assertThatThrownBy(() -> new PlaceVisitSearchCommand(1L, 101L, null, null, null, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_SIZE);
    }

    @Test
    void size가_음수면_예외를_던진다() {
        assertThatThrownBy(() -> new PlaceVisitSearchCommand(1L, 101L, null, null, null, -1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_SIZE);
    }

    @Test
    void size가_1_이상이면_정상_생성된다() {
        assertThatCode(() -> new PlaceVisitSearchCommand(1L, 101L, null, null, null, 1))
                .doesNotThrowAnyException();
    }
}
