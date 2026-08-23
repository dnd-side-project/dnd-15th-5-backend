package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisitedPlaceSearchCommandTest {

    @Test
    void 방문_장소_검색_조건을_생성할_때_keyword_양끝에_공백이_있으면_제거한다() {
        // when
        VisitedPlaceSearchCommand command = new VisitedPlaceSearchCommand(1L, "\u3000성수\u3000", null, 5);

        // then
        assertThat(command.keyword()).isEqualTo("성수");
    }

    @Test
    void 방문_장소_검색_조건을_생성할_때_cursor가_공백이면_null로_변환한다() {
        // when
        VisitedPlaceSearchCommand command = new VisitedPlaceSearchCommand(1L, "성수", "  ", 5);

        // then
        assertThat(command.cursor()).isNull();
    }

    @Test
    void 방문_장소_검색_조건을_생성할_때_size가_0이면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "카페", null, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_SIZE);
    }

    @Test
    void 방문_장소_검색_조건을_생성할_때_size가_6이면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "카페", null, 6))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_SIZE);
    }

    @Test
    void 방문_장소_검색_조건을_생성할_때_keyword가_공백이면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, " ", null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "\u3000", null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
    }

    @Test
    void 방문_장소_검색_조건을_생성할_때_keyword가_100자를_초과하면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> new VisitedPlaceSearchCommand(1L, "a".repeat(101), null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.INVALID_VISITED_PLACE_SEARCH_KEYWORD);
    }
}
