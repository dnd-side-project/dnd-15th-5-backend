package kr.chapchap.consumption.domain.service;

import kr.chapchap.consumption.domain.entity.PlacePopularityRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlacePopularityCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

    @Test
    void 최다_기록_카테고리를_선택하고_장소의_전체_기록수와_최근_방문일을_반환한다() {
        // given
        List<PlacePopularityRow> rows = List.of(
                new PlacePopularityRow(1L, "카페", 3L, TODAY.minusDays(10)),
                new PlacePopularityRow(1L, "쇼핑", 2L, TODAY),
                new PlacePopularityRow(2L, "취미/놀거리", 1L, TODAY)
        );

        // when
        List<PlacePopularityRow> result = PlacePopularityCalculator.aggregate(rows);

        // then
        assertThat(result).containsExactly(
                new PlacePopularityRow(1L, "카페", 5L, TODAY),
                new PlacePopularityRow(2L, "취미/놀거리", 1L, TODAY)
        );
    }

    @Test
    void 동률이면_입력순서나_최근_방문일과_무관하게_카테고리_이름순으로_선택한다() {
        // given
        PlacePopularityRow cafe = new PlacePopularityRow(1L, "카페", 2L, TODAY);
        PlacePopularityRow shopping = new PlacePopularityRow(1L, "쇼핑", 2L, TODAY.minusDays(1));

        // when
        List<PlacePopularityRow> forward = PlacePopularityCalculator.aggregate(List.of(cafe, shopping));
        List<PlacePopularityRow> reversed = PlacePopularityCalculator.aggregate(List.of(shopping, cafe));

        // then
        assertThat(forward).isEqualTo(reversed)
                .containsExactly(new PlacePopularityRow(1L, "쇼핑", 4L, TODAY));
    }

    @Test
    void 기록이_없으면_빈_결과를_반환한다() {
        // given
        List<PlacePopularityRow> rows = List.of();

        // when
        List<PlacePopularityRow> result = PlacePopularityCalculator.aggregate(rows);

        // then
        assertThat(result).isEmpty();
    }
}
