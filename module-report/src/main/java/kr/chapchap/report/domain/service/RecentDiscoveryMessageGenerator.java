package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.TimeSlot;
import kr.chapchap.report.domain.entity.VisitActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public class RecentDiscoveryMessageGenerator {

    private static final int MIN_ACTIVITY_COUNT_FOR_ZONE_COMPARISON = 3;
    private static final int NEW_PLACE_COUNT_THRESHOLD = 2;
    private static final int MIN_ACTIVITY_COUNT_FOR_TIME_COMPARISON = 3;
    private static final int MIN_ACTIVITY_COUNT_FOR_CATEGORY_COMPARISON = 3;
    private static final double CATEGORY_CONCENTRATION_THRESHOLD = 0.5;
    private static final double DAY_NIGHT_SHIFT_THRESHOLD = 1.5;

    private static final List<String> FALLBACK_MESSAGES = List.of(
            "이번 달 소비 기록을 꾸준히 남겨봐요",
            "오늘은 어떤 곳을 다녀오셨나요?"
    );

    public String generate(List<VisitActivity> recentActivities, List<VisitActivity> previousActivities) {
        List<String> candidates = new ArrayList<>();

        lifeZoneChangeMessage(recentActivities, previousActivities).ifPresent(candidates::add);
        newPlaceDiscoveryMessage(recentActivities, previousActivities).ifPresent(candidates::add);
        mostVisitedTimeSlotMessage(recentActivities).ifPresent(candidates::add);
        categoryConcentrationMessage(recentActivities).ifPresent(candidates::add);
        dayNightShiftMessage(recentActivities, previousActivities).ifPresent(candidates::add);

        List<String> pool = candidates.isEmpty() ? FALLBACK_MESSAGES : candidates;

        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    // 생활권 변화
    private Optional<String> lifeZoneChangeMessage(List<VisitActivity> recent, List<VisitActivity> previous) {
        if (recent.size() < MIN_ACTIVITY_COUNT_FOR_ZONE_COMPARISON || previous.size() < MIN_ACTIVITY_COUNT_FOR_ZONE_COMPARISON) {
            return Optional.empty();
        }
        Optional<String> recentDong = dominantDong(recent);
        Optional<String> previousDong = dominantDong(previous);

        if (recentDong.isEmpty() || previousDong.isEmpty() || recentDong.equals(previousDong)) {
            return Optional.empty();
        }

        return Optional.of("최근 "+previousDong.get() + "에서 " + recentDong.get() + "으로 본거지를 이동중이시네요!");
    }

    private Optional<String> dominantDong(List<VisitActivity> activities) {
        return activities.stream()
                .map(VisitActivity::dongName)
                .filter(dongName -> dongName != null && !dongName.isBlank())
                .collect(Collectors.groupingBy(dongName -> dongName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    // 신규 장소 방문
    private Optional<String> newPlaceDiscoveryMessage(List<VisitActivity> recent, List<VisitActivity> previous) {
        Set<Long> previousPlaceIds = previous.stream()
                .map(VisitActivity::placeId)
                .collect(Collectors.toSet());

        Set<Long> newPlaceIds = new HashSet<>();
        for (VisitActivity activity : recent) {
            if (!previousPlaceIds.contains(activity.placeId())) {
                newPlaceIds.add(activity.placeId());
            }
        }

        if (newPlaceIds.size() < NEW_PLACE_COUNT_THRESHOLD) {
            return Optional.empty();
        }

        return Optional.of("프로 단골러 등장! 최근 새로운 단골집이 " + newPlaceIds.size() + "곳이나 늘어났어요");
    }

    private Optional<String> mostVisitedTimeSlotMessage(List<VisitActivity> recent) {
        List<VisitActivity> withTime = recent.stream()
                .filter(activity -> activity.purchaseTime() != null)
                .toList();

        if (withTime.size() < MIN_ACTIVITY_COUNT_FOR_TIME_COMPARISON) {
            return Optional.empty();
        }

        Optional<TimeSlot> mostVisitedSlot = withTime.stream()
                .collect(Collectors.groupingBy(
                        activity -> TimeSlot.from(activity.purchaseTime()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (mostVisitedSlot.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of("요즘은 " + mostVisitedSlot.get().getLabel() + " 방문이 가장 많아요");
    }

    // 카테고리 쏠림
    private Optional<String> categoryConcentrationMessage(List<VisitActivity> recent) {
        List<VisitActivity> withCategory = recent.stream()
                .filter(activity -> activity.category() != null && !activity.category().isBlank())
                .toList();

        if (withCategory.size() < MIN_ACTIVITY_COUNT_FOR_CATEGORY_COMPARISON) {
            return Optional.empty();
        }

        Optional<Map.Entry<String, Long>> dominantCategory = withCategory.stream()
                .collect(Collectors.groupingBy(VisitActivity::category, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (dominantCategory.isEmpty()) {
            return Optional.empty();
        }

        double ratio = dominantCategory.get().getValue() / (double) withCategory.size();
        if (ratio < CATEGORY_CONCENTRATION_THRESHOLD) {
            return Optional.empty();
        }

        long percentage = Math.round(ratio * 100);
        return Optional.of("최근 지출의 " + percentage + "%가 " + dominantCategory.get().getKey() + "에 집중되는중!");
    }

    // 낮/밤 소비 성향 변화 (페르소나 축 산정과 동일한 -2~+2 시간대 가중치를 사용)
    private Optional<String> dayNightShiftMessage(List<VisitActivity> recent, List<VisitActivity> previous) {
        OptionalDouble recentAverage = averageTimeWeight(recent);
        OptionalDouble previousAverage = averageTimeWeight(previous);

        if (recentAverage.isEmpty() || previousAverage.isEmpty()) {
            return Optional.empty();
        }

        double shift = recentAverage.getAsDouble() - previousAverage.getAsDouble();
        if (shift >= DAY_NIGHT_SHIFT_THRESHOLD) {
            return Optional.of("요즘 올빼미 모드 켜졌나요? 밤 활동 비중이 훌쩍 늘었어요");
        }
        if (shift <= -DAY_NIGHT_SHIFT_THRESHOLD) {
            return Optional.of("요새 낮 활동이 확 늘었네요! 햇살 좀 쬐면서 비타민 D " + "충전 중이신가요?");
        }
        return Optional.empty();
    }

    private OptionalDouble averageTimeWeight(List<VisitActivity> activities) {
        List<VisitActivity> withTime = activities.stream()
                .filter(activity -> activity.purchaseTime() != null)
                .toList();

        if (withTime.size() < MIN_ACTIVITY_COUNT_FOR_TIME_COMPARISON) {
            return OptionalDouble.empty();
        }

        return withTime.stream()
                .mapToInt(activity -> TimeOfDayWeightCalculator.hourWeight(activity.purchaseTime().getHour()))
                .average();
    }
}
