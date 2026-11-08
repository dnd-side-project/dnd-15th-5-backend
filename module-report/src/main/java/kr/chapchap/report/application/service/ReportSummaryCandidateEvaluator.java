package kr.chapchap.report.application.service;

import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.SummaryCandidateInfo;
import kr.chapchap.report.application.info.SummaryEvaluationResult;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.DongNameLookupPort;
import kr.chapchap.report.application.port.PlaceNameLookupPort;
import kr.chapchap.report.domain.entity.SummaryHighlightType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReportSummaryCandidateEvaluator {

    private static final double REVISIT_RATIO_THRESHOLD = 0.60;
    private static final double NEW_PLACE_RATIO_THRESHOLD = 0.70;
    private static final double MAX_CATEGORY_RATIO_THRESHOLD = 0.50;
    private static final double MAX_DONG_RATIO_THRESHOLD = 0.50;
    private static final int DIFFERENT_DONG_THRESHOLD = 3;
    private static final int DIFFERENT_CATEGORY_THRESHOLD = 4;
    private static final int FAVORITE_PLACE_VISIT_THRESHOLD = 5;
    private static final int CONSISTENT_MONTHS = 3;

    private final ConsumptionActivityPort consumptionActivityPort;
    private final DongNameLookupPort dongNameLookupPort;
    private final PlaceNameLookupPort placeNameLookupPort;
    private final Clock clock;

    public SummaryEvaluationResult evaluate(Long userId) {
        LocalDate today = LocalDate.now(clock);

        Optional<YearMonth> firstAvailable = consumptionActivityPort.findFirstAvailableYearMonth(userId);
        if (firstAvailable.isEmpty()) {
            return new SummaryEvaluationResult(0, 0, List.of());
        }

        List<ConsumptionActivity> allActivities = consumptionActivityPort.findActivities(userId, firstAvailable.get().atDay(1), today.plusDays(1));

        int totalEverCount = allActivities.size();
        if (totalEverCount == 0) {
            return new SummaryEvaluationResult(0, 0, List.of());
        }

        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate recentThreeMonthsStart = currentMonth.minusMonths(CONSISTENT_MONTHS - 1).atDay(1);

        List<ConsumptionActivity> monthActivities = filterByRange(allActivities, monthStart, today);
        List<ConsumptionActivity> recentThreeMonthsActivities = filterByRange(allActivities, recentThreeMonthsStart, today);

        int recentThreeMonthsDistinctPlaceCount = (int) recentThreeMonthsActivities.stream()
                .map(ConsumptionActivity::placeId).distinct().count();

        if (monthActivities.isEmpty() && recentThreeMonthsActivities.isEmpty()) {
            return new SummaryEvaluationResult(totalEverCount, recentThreeMonthsDistinctPlaceCount, List.of());
        }

        // 가게별 전체 이력 - 최초방문일/재방문 여부 계산용
        Map<Long, List<ConsumptionActivity>> allByPlace = allActivities.stream()
                .sorted(Comparator.comparing(ConsumptionActivity::purchaseDate)
                        .thenComparing(ConsumptionActivity::purchaseTime, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(ConsumptionActivity::placeId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, LocalDate> earliestVisitDateByPlace = allByPlace.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0).purchaseDate()));

        List<Long> dongLookupPlaceIds = Stream.concat(
                        monthActivities.stream().map(ConsumptionActivity::placeId),
                        recentThreeMonthsActivities.stream().map(ConsumptionActivity::placeId))
                .distinct().toList();

        Map<Long, String> dongNamesByPlace = dongNameLookupPort.findDongNames(dongLookupPlaceIds);

        List<SummaryCandidateInfo> candidates = new ArrayList<>();
        addIfEligible(candidates, evaluateExplorer(monthActivities, earliestVisitDateByPlace, monthStart));
        addIfEligible(candidates, evaluateTasteFocused(monthActivities));
        addIfEligible(candidates, evaluateAreaVisitor(monthActivities, dongNamesByPlace));
        addIfEligible(candidates, evaluateDiverseTaste(monthActivities));
        addIfEligible(candidates, evaluateLoyal(recentThreeMonthsActivities, allByPlace));
        addIfEligible(candidates, evaluateLocalFocused(recentThreeMonthsActivities, dongNamesByPlace));
        addIfEligible(candidates, evaluateConsistentRecorder(allActivities, currentMonth));
        addIfEligible(candidates, evaluateFavoritePlace(allByPlace));

        return new SummaryEvaluationResult(totalEverCount, recentThreeMonthsDistinctPlaceCount, candidates);
    }

    private List<ConsumptionActivity> filterByRange(List<ConsumptionActivity> activities, LocalDate from, LocalDate toInclusive) {
        return activities.stream()
                .filter(activity -> !activity.purchaseDate().isBefore(from) && !activity.purchaseDate().isAfter(toInclusive))
                .toList();
    }

    private void addIfEligible(List<SummaryCandidateInfo> candidates, SummaryCandidateInfo candidate) {
        if (candidate != null) {
            candidates.add(candidate);
        }
    }

    // 탐험형: 이번 달 방문한 고유 가게 중 "최초 방문일이 이번 달"인 비율 >= 70%
    private SummaryCandidateInfo evaluateExplorer(List<ConsumptionActivity> monthActivities, Map<Long, LocalDate> earliestVisitDateByPlace, LocalDate monthStart) {
        List<Long> distinctPlaceIds = monthActivities.stream().map(ConsumptionActivity::placeId).distinct().toList();
        if (distinctPlaceIds.isEmpty()) {
            return null;
        }
        long newPlaceCount = distinctPlaceIds.stream()
                .filter(placeId -> !earliestVisitDateByPlace.get(placeId).isBefore(monthStart))
                .count();
        double ratio = (double) newPlaceCount / distinctPlaceIds.size();

        if (ratio < NEW_PLACE_RATIO_THRESHOLD) {
            return null;
        }

        return new SummaryCandidateInfo(
                SummaryHighlightType.EXPLORER,
                "이번 달은 새로운 곳을 많이 발견했네요!",
                "이번 달 방문한 %d곳 중 %d곳이 처음 기록한 가게에요.".formatted(distinctPlaceIds.size(), newPlaceCount));
    }

    // 취향 집중형: 이번 달 최다 카테고리 기록 비율 >= 50%
    private SummaryCandidateInfo evaluateTasteFocused(List<ConsumptionActivity> monthActivities) {
        if (monthActivities.isEmpty()) {
            return null;
        }
        Map.Entry<String, Long> top = topByCount(monthActivities, ConsumptionActivity::category);
        if (top == null) {
            return null;
        }
        double ratio = (double) top.getValue() / monthActivities.size();
        if (ratio < MAX_CATEGORY_RATIO_THRESHOLD) {
            return null;
        }
        int percentage = (int) Math.round(ratio * 100);

        return new SummaryCandidateInfo(
                SummaryHighlightType.TASTE_FOCUSED,
                "이번 달은 %s에 푹 빠지셨네요!".formatted(top.getKey()),
                "이번 달 방문의 %d%%가 %s예요.".formatted(percentage, top.getKey()));
    }

    // 방문 지역형: 이번 달 서로 다른 동네 수 >= 3
    private SummaryCandidateInfo evaluateAreaVisitor(List<ConsumptionActivity> monthActivities, Map<Long, String> dongNamesByPlace) {

        long distinctDongCount = monthActivities.stream()
                .map(ConsumptionActivity::placeId)
                .map(dongNamesByPlace::get)
                .filter(dongName -> dongName != null && !dongName.isBlank())
                .distinct()
                .count();

        if (distinctDongCount < DIFFERENT_DONG_THRESHOLD) {
            return null;
        }

        return new SummaryCandidateInfo(
                SummaryHighlightType.AREA_VISITOR,
                "이번 달은 여러 동네를 돌아다니셨네요!",
                "이번 달 %d개 동네에 방문 기록을 남겼어요.".formatted(distinctDongCount));
    }

    // 다양한 취향형: 이번 달 서로 다른 카테고리 수 >= 4
    private SummaryCandidateInfo evaluateDiverseTaste(List<ConsumptionActivity> monthActivities) {
        long distinctCategoryCount = monthActivities.stream().map(ConsumptionActivity::category).distinct().count();

        if (distinctCategoryCount < DIFFERENT_CATEGORY_THRESHOLD) {
            return null;
        }

        return new SummaryCandidateInfo(
                SummaryHighlightType.DIVERSE_TASTE,
                "이번 달은 취향이 다양했네요!",
                "이번 달 %d개 카테고리에서 기록을 남겼어요.".formatted(distinctCategoryCount));
    }

    // 단골형: 최근 3개월 재방문(같은 가게 전체 이력상 2번째 이후) 기록 비율 >= 60%
    private SummaryCandidateInfo evaluateLoyal(List<ConsumptionActivity> recentThreeMonthsActivities, Map<Long, List<ConsumptionActivity>> allByPlace) {
        if (recentThreeMonthsActivities.isEmpty()) {
            return null;
        }

        long revisitCount = recentThreeMonthsActivities.stream()
                .filter(activity -> isRevisit(activity, allByPlace.get(activity.placeId())))
                .count();
        double ratio = (double) revisitCount / recentThreeMonthsActivities.size();

        if (ratio < REVISIT_RATIO_THRESHOLD) {
            return null;
        }

        return new SummaryCandidateInfo(
                SummaryHighlightType.LOYAL,
                "익숙한 가게를 자주 찾으시네요!",
                "최근 3개월 방문 %d건 중 %d건이 재방문이에요.".formatted(recentThreeMonthsActivities.size(), revisitCount));
    }

    private boolean isRevisit(ConsumptionActivity activity, List<ConsumptionActivity> placeHistory) {
        if (placeHistory == null || placeHistory.isEmpty()) {
            return false;
        }
        ConsumptionActivity earliest = placeHistory.get(0);

        if (!earliest.purchaseDate().equals(activity.purchaseDate())) {
            return true;
        }
        return !Objects.equals(earliest.purchaseTime(), activity.purchaseTime());
    }

    // 동네 집중형: 최근 3개월 최다 동네(행정동) 기록 비율 >= 50%
    private SummaryCandidateInfo evaluateLocalFocused(
            List<ConsumptionActivity> recentThreeMonthsActivities, Map<Long, String> dongNamesByPlace
    ) {
        if (recentThreeMonthsActivities.isEmpty()) {
            return null;
        }

        List<String> dongNames = recentThreeMonthsActivities.stream()
                .map(ConsumptionActivity::placeId)
                .map(dongNamesByPlace::get)
                .filter(dongName -> dongName != null && !dongName.isBlank())
                .toList();

        if (dongNames.isEmpty()) {
            return null;
        }

        Map.Entry<String, Long> top = topByCount(dongNames);
        if (top == null) {
            return null;
        }
        double ratio = (double) top.getValue() / recentThreeMonthsActivities.size();
        if (ratio < MAX_DONG_RATIO_THRESHOLD) {
            return null;
        }
        return new SummaryCandidateInfo(SummaryHighlightType.LOCAL_FOCUSED,
                "%s을 자주 찾으시네요!".formatted(top.getKey()),
                "최근 3개월 방문 %d회 중 %d회 %s을 방문했어요.".formatted(recentThreeMonthsActivities.size(), top.getValue(), top.getKey()));
    }

    // 꾸준한 기록형: 최근 3개 달각각 최소 1건
    private SummaryCandidateInfo evaluateConsistentRecorder(List<ConsumptionActivity> allActivities, YearMonth currentMonth) {
        for (int i = 0; i < CONSISTENT_MONTHS; i++) {
            YearMonth target = currentMonth.minusMonths(i);
            boolean hasRecord = allActivities.stream()
                    .anyMatch(activity -> YearMonth.from(activity.purchaseDate()).equals(target));
            if (!hasRecord) {
                return null;
            }
        }
        return new SummaryCandidateInfo(
                SummaryHighlightType.CONSISTENT_RECORDER,
                "벌써 3개월째 기록을 쌓고 있네요!",
                "최근 3개 달 연속 기록을 남겼어요.");
    }

    // 최애 가게형: 전체 누적 방문 가장 많은 가게가 5회 이상.
    private SummaryCandidateInfo evaluateFavoritePlace(Map<Long, List<ConsumptionActivity>> allByPlace) {

        Optional<Map.Entry<Long, List<ConsumptionActivity>>> favorite = allByPlace.entrySet().stream()
                .max(Comparator.<Map.Entry<Long, List<ConsumptionActivity>>>comparingInt(e -> e.getValue().size())
                        .thenComparing(e -> e.getValue().get(e.getValue().size() - 1).purchaseDate()));

        if (favorite.isEmpty() || favorite.get().getValue().size() < FAVORITE_PLACE_VISIT_THRESHOLD) {
            return null;
        }

        Long placeId = favorite.get().getKey();
        int visitCount = favorite.get().getValue().size();
        String placeName = placeNameLookupPort.findPlaceNames(List.of(placeId)).get(placeId);

        if (placeName == null) {
            return null;
        }

        return new SummaryCandidateInfo(
                SummaryHighlightType.FAVORITE_PLACE,
                "가장 많이 찾은 곳은 %s이에요!".formatted(placeName),
                "지금까지 %s을 %d번 방문했어요.".formatted(placeName, visitCount));
    }

    private <T> Map.Entry<T, Long> topByCount(List<T> items) {
        return items.stream()
                .collect(Collectors.groupingBy(item -> item, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private <T> Map.Entry<T, Long> topByCount(List<ConsumptionActivity> activities, Function<ConsumptionActivity, T> keyFn) {
        return topByCount(activities.stream().map(keyFn).toList());
    }
}
