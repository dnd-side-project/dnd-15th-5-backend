package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.MonthlyAggregationResult;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedTimePattern;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedTownRank;
import kr.chapchap.report.domain.entity.MonthlyVisitActivity;
import kr.chapchap.report.domain.entity.PersonaScoreResult;
import kr.chapchap.report.domain.entity.PersonaType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaAxisScoringServiceTest {

    private final PersonaAxisScoringService sut = new PersonaAxisScoringService();

    @Test
    void 재방문율이_50퍼센트_이상이면_단골형이다() {
        // given: place 1(기존), place 2(기존) 재방문 + place 3(신규) 1건 = 재방문율 2/3 ≈ 66.7%
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "A동", "가게1", LocalDate.of(2026, 7, 1), LocalTime.of(12, 0)),
                activity(2L, "A동", "가게2", LocalDate.of(2026, 7, 2), LocalTime.of(12, 0)),
                activity(3L, "A동", "가게3", LocalDate.of(2026, 7, 3), LocalTime.of(12, 0))
        );
        Set<Long> priorVisitedPlaceIds = Set.of(1L, 2L);
        MonthlyAggregationResult aggregation = aggregationOf(3, List.of(new AggregatedTownRank(1, "A동", 3)), List.of());

        // when
        PersonaScoreResult result = sut.score(activities, priorVisitedPlaceIds, aggregation);

        // then
        assertThat(result.personaType().getVisitStyle()).isEqualTo(PersonaType.VisitStyle.REGULAR);
    }

    @Test
    void 재방문율이_50퍼센트_미만이면_탐험형이다() {
        // given: 전부 신규 방문 = 재방문율 0%
        List<MonthlyVisitActivity> activities = List.of(
                activity(1L, "A동", "가게1", LocalDate.of(2026, 7, 1), LocalTime.of(12, 0))
        );
        MonthlyAggregationResult aggregation = aggregationOf(1, List.of(new AggregatedTownRank(1, "A동", 1)), List.of());

        // when
        PersonaScoreResult result = sut.score(activities, Set.of(), aggregation);

        // then
        assertThat(result.personaType().getVisitStyle()).isEqualTo(PersonaType.VisitStyle.NOMAD);
    }

    @Test
    void 최다_방문_동네_비중이_50퍼센트_이상이면_한동네형이다() {
        // given: 총 10건 중 A동이 6건(60%)
        MonthlyAggregationResult aggregation = aggregationOf(10, List.of(new AggregatedTownRank(1, "A동", 6)), List.of());

        // when
        PersonaScoreResult result = sut.score(List.of(), Set.of(), aggregation);

        // then
        assertThat(result.personaType().getActivityRange()).isEqualTo(PersonaType.ActivityRange.HOME);
    }

    @Test
    void 최다_방문_동네_비중이_50퍼센트_미만이면_확장형이다() {
        // given: 총 10건 중 A동이 4건(40%)
        MonthlyAggregationResult aggregation = aggregationOf(10, List.of(new AggregatedTownRank(1, "A동", 4)), List.of());

        // when
        PersonaScoreResult result = sut.score(List.of(), Set.of(), aggregation);

        // then
        assertThat(result.personaType().getActivityRange()).isEqualTo(PersonaType.ActivityRange.WANDER);
    }

    @Test
    void 밤_시간대_방문이_많으면_밤형이고_충동성_상위_요일_비중이_낮으면_즉흥형이다() {
        // given: 밤(21시, 가중치 +2) 방문이지만, 요일 5개에 2건씩 고르게 퍼져있어 상위 2개 요일 비중이 40%(60% 미만)
        List<AggregatedTimePattern> timePatterns = List.of(
                new AggregatedTimePattern(1, 21, 2),
                new AggregatedTimePattern(2, 21, 2),
                new AggregatedTimePattern(3, 22, 2),
                new AggregatedTimePattern(4, 22, 2),
                new AggregatedTimePattern(5, 23, 2)
        );
        MonthlyAggregationResult aggregation = aggregationOf(10, List.of(new AggregatedTownRank(1, "A동", 10)), timePatterns);

        // when
        PersonaScoreResult result = sut.score(List.of(), Set.of(), aggregation);

        // then
        assertThat(result.personaType().getConsumptionTime()).isEqualTo(PersonaType.ConsumptionTime.MOON);
        assertThat(result.personaType().getConsumptionRhythm()).isEqualTo(PersonaType.ConsumptionRhythm.FREE);
    }

    @Test
    void 낮_시간대_방문이_많으면_낮형이고_특정_요일에_몰려있으면_루틴형이다() {
        // given: 오전(9시, 가중치 -2) 방문이 몰려있고, 화요일 하나에 몰려있어 상위 2개 요일 비중이 60% 이상
        List<AggregatedTimePattern> timePatterns = List.of(
                new AggregatedTimePattern(2, 9, 8),
                new AggregatedTimePattern(4, 9, 2)
        );
        MonthlyAggregationResult aggregation = aggregationOf(10, List.of(new AggregatedTownRank(1, "A동", 10)), timePatterns);

        // when
        PersonaScoreResult result = sut.score(List.of(), Set.of(), aggregation);

        // then
        assertThat(result.personaType().getConsumptionTime()).isEqualTo(PersonaType.ConsumptionTime.DAY);
        assertThat(result.personaType().getConsumptionRhythm()).isEqualTo(PersonaType.ConsumptionRhythm.PATTERN);
    }

    @Test
    void 방문_시각_기록이_없으면_낮형_기본값으로_처리한다() {
        // given
        MonthlyAggregationResult aggregation = aggregationOf(3, List.of(new AggregatedTownRank(1, "A동", 3)), List.of());

        // when
        PersonaScoreResult result = sut.score(List.of(), Set.of(), aggregation);

        // then
        assertThat(result.personaType().getConsumptionTime()).isEqualTo(PersonaType.ConsumptionTime.DAY);
    }

    private MonthlyVisitActivity activity(Long placeId, String dongName, String placeName, LocalDate purchaseDate, LocalTime purchaseTime) {
        return new MonthlyVisitActivity(placeId, dongName, placeName, "CAFE", purchaseDate, purchaseTime);
    }

    private MonthlyAggregationResult aggregationOf(int totalVisitCount, List<AggregatedTownRank> townRanks, List<AggregatedTimePattern> timePatterns) {
        return new MonthlyAggregationResult(totalVisitCount, 0, 0, List.of(), townRanks, List.of(), timePatterns);
    }
}
