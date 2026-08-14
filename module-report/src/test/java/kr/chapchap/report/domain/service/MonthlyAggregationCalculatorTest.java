package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.MonthlyAggregationResult;
import kr.chapchap.report.domain.entity.MonthlyVisitActivity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyAggregationCalculatorTest {

    private final MonthlyAggregationCalculator sut = new MonthlyAggregationCalculator();

    @Test
    void 카테고리별_방문_비율을_백분율로_계산한다() {
        // given
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "신논현동", "가게A", "CAFE", LocalDate.of(2026, 7, 1), LocalTime.of(10, 0)),
                activity(1L, "신논현동", "가게A", "CAFE", LocalDate.of(2026, 7, 2), LocalTime.of(10, 0)),
                activity(2L, "신논현동", "가게B", "RESTAURANT", LocalDate.of(2026, 7, 3), LocalTime.of(12, 0))
        );

        // when
        MonthlyAggregationResult result = sut.calculate(activities, Set.of(), Set.of(), Map.of());

        // then
        assertThat(result.categoryStats())
                .extracting(MonthlyAggregationResult.AggregatedCategoryStat::category,
                        MonthlyAggregationResult.AggregatedCategoryStat::percentage)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("CAFE", new BigDecimal("66.67")),
                        org.assertj.core.groups.Tuple.tuple("RESTAURANT", new BigDecimal("33.33"))
                );
    }

    @Test
    void 동네_랭킹은_방문수_내림차순_상위_3개까지만_반환한다() {
        // given
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "A동", "가게1", "CAFE", LocalDate.of(2026, 7, 1), null),
                activity(1L, "A동", "가게1", "CAFE", LocalDate.of(2026, 7, 2), null),
                activity(2L, "B동", "가게2", "CAFE", LocalDate.of(2026, 7, 3), null),
                activity(3L, "C동", "가게3", "CAFE", LocalDate.of(2026, 7, 4), null),
                activity(4L, "D동", "가게4", "CAFE", LocalDate.of(2026, 7, 5), null)
        );

        // when
        MonthlyAggregationResult result = sut.calculate(activities, Set.of(), Set.of(), Map.of());

        // then
        assertThat(result.townRanks()).hasSize(3);
        assertThat(result.townRanks().get(0).townName()).isEqualTo("A동");
        assertThat(result.townRanks().get(0).rank()).isEqualTo(1);
        assertThat(result.townRanks().get(0).visitCount()).isEqualTo(2);
    }

    @Test
    void 이전에_방문한_적_없는_동네와_가게만_신규로_집계한다() {
        // given
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "기존동", "기존가게", "CAFE", LocalDate.of(2026, 7, 1), null),
                activity(2L, "신규동", "신규가게", "CAFE", LocalDate.of(2026, 7, 2), null)
        );
        Set<Long> priorPlaceIds = Set.of(1L);
        Set<String> priorTownNames = Set.of("기존동");

        // when
        MonthlyAggregationResult result = sut.calculate(activities, priorPlaceIds, priorTownNames, Map.of());

        // then
        assertThat(result.newTownCount()).isEqualTo(1);
        assertThat(result.newPlaceCount()).isEqualTo(1);
    }

    @Test
    void 장소_랭킹에_최초_방문일을_반영한다() {
        // given
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "A동", "가게1", "CAFE", LocalDate.of(2026, 7, 10), null)
        );
        Map<Long, LocalDate> earliestVisitDateByPlaceId = Map.of(1L, LocalDate.of(2026, 5, 1));

        // when
        MonthlyAggregationResult result = sut.calculate(activities, Set.of(), Set.of(), earliestVisitDateByPlaceId);

        // then
        assertThat(result.placeRanks()).hasSize(1);
        assertThat(result.placeRanks().get(0).firstVisitedDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void 방문_시각이_없는_활동은_시간대_패턴_집계에서_제외한다() {
        // given
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "A동", "가게1", "CAFE", LocalDate.of(2026, 7, 6), LocalTime.of(20, 0)), // 월요일 20시
                activity(2L, "B동", "가게2", "CAFE", LocalDate.of(2026, 7, 7), null)
        );

        // when
        MonthlyAggregationResult result = sut.calculate(activities, Set.of(), Set.of(), Map.of());

        // then
        assertThat(result.timePatterns()).hasSize(1);
        assertThat(result.timePatterns().get(0).dayOfWeek()).isEqualTo(1);
        assertThat(result.timePatterns().get(0).visitHour()).isEqualTo(20);
    }

    private MonthlyVisitActivity activity(Long placeId, String dongName, String placeName, String category,
                                           LocalDate purchaseDate, LocalTime purchaseTime) {
        return new MonthlyVisitActivity(placeId, dongName, placeName, category, purchaseDate, purchaseTime);
    }
}
