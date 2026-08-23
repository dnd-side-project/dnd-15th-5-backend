package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisitedPlaceSearchCommandTest {

    @Test
    void 검색어의_양끝_공백을_제거하고_빈_커서는_첫_페이지로_정규화한다() {
        // when
        VisitedPlaceSearchCommand command = new VisitedPlaceSearchCommand(1L, "\u3000성수\u3000", "  ", 5);

        // then
        assertThat(command.keyword()).isEqualTo("성수");
        assertThat(command.cursor()).isNull();
    }

    @Test
    void size가_1보다_작거나_5보다_크면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "카페", null, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_SIZE);
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "카페", null, 6))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_SIZE);
    }

    @Test
    void 검색어가_비었거나_100자를_초과하면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, " ", null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "\u3000", null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "a".repeat(101), null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
    }
}
