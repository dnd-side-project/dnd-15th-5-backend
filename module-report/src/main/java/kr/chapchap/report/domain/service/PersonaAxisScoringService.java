package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.MonthlyAggregationResult;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedTimePattern;
import kr.chapchap.report.domain.entity.MonthlyAggregationResult.AggregatedTownRank;
import kr.chapchap.report.domain.entity.MonthlyVisitActivity;
import kr.chapchap.report.domain.entity.PersonaScoreResult;
import kr.chapchap.report.domain.entity.PersonaType;
import kr.chapchap.report.domain.entity.PersonaType.ActivityRange;
import kr.chapchap.report.domain.entity.PersonaType.ConsumptionRhythm;
import kr.chapchap.report.domain.entity.PersonaType.ConsumptionTime;
import kr.chapchap.report.domain.entity.PersonaType.VisitStyle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class PersonaAxisScoringService implements PersonaScoringService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCORE_SCALE = 2;

    private static final BigDecimal VISIT_STYLE_THRESHOLD = BigDecimal.valueOf(50);//재방문율
    private static final BigDecimal ACTIVITY_RANGE_THRESHOLD = BigDecimal.valueOf(50); //최다 방문동 비중
    private static final BigDecimal RHYTHM_THRESHOLD = BigDecimal.valueOf(60);// TOP 2 요일 비중

    private static final int MORNING_START = 5;
    private static final int AFTERNOON_START = 11;
    private static final int EVENING_START = 17;
    private static final int NIGHT_START = 21;

    private static final int MORNING_WEIGHT = -2;
    private static final int AFTERNOON_WEIGHT = -1;
    private static final int EVENING_WEIGHT = 1;
    private static final int NIGHT_WEIGHT = 2;

    private static final int TOP_DAYS_FOR_RHYTHM = 2; // 상위 2개 요일 추출

    @Override
    public PersonaScoreResult score(List<MonthlyVisitActivity> monthActivities, Set<Long> priorVisitedPlaceIds, MonthlyAggregationResult aggregationResult) {
        int totalVisitCount = aggregationResult.totalVisitCount();

        //재방문율
        BigDecimal revisitRatio = calculateRevisitRatio(monthActivities, priorVisitedPlaceIds, totalVisitCount);
        VisitStyle visitStyle = revisitRatio.compareTo(VISIT_STYLE_THRESHOLD) >= 0 ? VisitStyle.REGULAR : VisitStyle.NOMAD;

        //최다 방문 동
        BigDecimal dominantTownShare = calculateDominantTownShare(aggregationResult, totalVisitCount);
        ActivityRange activityRange = dominantTownShare.compareTo(ACTIVITY_RANGE_THRESHOLD) >= 0 ? ActivityRange.HOME : ActivityRange.WANDER;

        //시간대 평균
        TimeIntensity timeIntensity = calculateTimeIntensity(aggregationResult.timePatterns());
        ConsumptionTime consumptionTime = timeIntensity.averageWeight().signum() > 0 ? ConsumptionTime.MOON : ConsumptionTime.DAY;

        //요일 집중도
        BigDecimal topDaysShare = calculateTopDaysShare(aggregationResult.timePatterns());
        ConsumptionRhythm consumptionRhythm = topDaysShare.compareTo(RHYTHM_THRESHOLD) >= 0 ? ConsumptionRhythm.PATTERN : ConsumptionRhythm.FREE;

        PersonaType personaType = PersonaType.of(visitStyle, activityRange, consumptionTime, consumptionRhythm);

        return new PersonaScoreResult(
                personaType,
                HUNDRED.subtract(revisitRatio).setScale(SCORE_SCALE, RoundingMode.HALF_UP),
                HUNDRED.subtract(dominantTownShare).setScale(SCORE_SCALE, RoundingMode.HALF_UP),
                timeIntensity.toDaytimeScore(),
                HUNDRED.subtract(topDaysShare).setScale(SCORE_SCALE, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateRevisitRatio(List<MonthlyVisitActivity> monthActivities, Set<Long> priorVisitedPlaceIds, int totalVisitCount) {
        if (totalVisitCount == 0) {
            return BigDecimal.ZERO;
        }
        long revisitCount = monthActivities.stream()
                .filter(activity -> priorVisitedPlaceIds.contains(activity.placeId()))
                .count();
        return percentage(revisitCount, totalVisitCount);
    }

    private BigDecimal calculateDominantTownShare(MonthlyAggregationResult aggregationResult, int totalVisitCount) {
        if (totalVisitCount == 0 || aggregationResult.townRanks().isEmpty()) {
            return BigDecimal.ZERO;
        }
        AggregatedTownRank dominantTown = aggregationResult.townRanks().get(0);
        return percentage(dominantTown.visitCount(), totalVisitCount);
    }

    private TimeIntensity calculateTimeIntensity(List<AggregatedTimePattern> timePatterns) {
        long weightedSum = 0;
        long timedVisitCount = 0;
        for (AggregatedTimePattern pattern : timePatterns) {
            weightedSum += (long) hourWeight(pattern.visitHour()) * pattern.visitCount();
            timedVisitCount += pattern.visitCount();
        }
        return new TimeIntensity(weightedSum, timedVisitCount);
    }

    private int hourWeight(int visitHour) {
        if (visitHour >= MORNING_START && visitHour < AFTERNOON_START) {
            return MORNING_WEIGHT;
        }
        if (visitHour >= AFTERNOON_START && visitHour < EVENING_START) {
            return AFTERNOON_WEIGHT;
        }
        if (visitHour >= EVENING_START && visitHour < NIGHT_START) {
            return EVENING_WEIGHT;
        }
        return NIGHT_WEIGHT;
    }


    private BigDecimal calculateTopDaysShare(List<AggregatedTimePattern> timePatterns) {
        Map<Integer, Long> visitCountByDayOfWeek = timePatterns.stream()
                .collect(Collectors.groupingBy(AggregatedTimePattern::dayOfWeek,
                        Collectors.summingLong(AggregatedTimePattern::visitCount)));

        long totalTimedVisits = visitCountByDayOfWeek.values().stream().mapToLong(Long::longValue).sum();
        if (totalTimedVisits == 0) {
            return BigDecimal.ZERO;
        }

        long topDaysSum = visitCountByDayOfWeek.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(TOP_DAYS_FOR_RHYTHM)
                .mapToLong(Long::longValue)
                .sum();

        return percentage(topDaysSum, totalTimedVisits);
    }

    private BigDecimal percentage(long part, long total) {
        return BigDecimal.valueOf(part)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private record TimeIntensity(long weightedSum, long timedVisitCount) {

        private BigDecimal averageWeight() {
            if (timedVisitCount == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(weightedSum)
                    .divide(BigDecimal.valueOf(timedVisitCount), 4, RoundingMode.HALF_UP);
        }

        private BigDecimal toDaytimeScore() {
            BigDecimal clamped = averageWeight().max(BigDecimal.valueOf(-2)).min(BigDecimal.valueOf(2));
            return BigDecimal.valueOf(2).subtract(clamped)
                    .divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}
