package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.VisitActivity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public class RecentDiscoveryMessageGenerator {

    private static final int MIN_ACTIVITY_COUNT_FOR_ZONE_COMPARISON = 3;
    private static final int NEW_PLACE_COUNT_THRESHOLD = 2;
    private static final int MIN_ACTIVITY_COUNT_FOR_TIME_COMPARISON = 3;

    private static final List<String> FALLBACK_MESSAGES = List.of(
            "이번 달 소비 기록을 꾸준히 남겨봐요",
            "오늘은 어떤 곳을 다녀오셨나요?"
    );

    private enum TimeSlot{
        DAWN("새벽"),
        MORNING("아침"),
        LUNCH("점심"),
        EVENING("저녁"),
        NIGHT("밤");

        private final String label;

        TimeSlot(String label){
            this.label=label;
        }

        public String getLabel(){
            return label;
        }

        public static TimeSlot from(LocalTime time) {
            int hour = time.getHour();
            if (hour >= 2 && hour < 6) {
                return DAWN;
            }
            if (hour >= 6 && hour < 11) {
                return MORNING;
            }
            if (hour >= 11 && hour < 16) {
                return LUNCH;
            }
            if (hour >=16 && hour < 21 )
                return EVENING;
            return NIGHT;
        }
    }

    public String generate(List<VisitActivity> recentActivities, List<VisitActivity> previousActivities) {
        List<String> candidates = new ArrayList<>();

        lifeZoneChangeMessage(recentActivities, previousActivities).ifPresent(candidates::add);
        newPlaceDiscoveryMessage(recentActivities, previousActivities).ifPresent(candidates::add);
        mostVisitedTimeSlotMessage(recentActivities).ifPresent(candidates::add);

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

        return Optional.of(previousDong.get() + "에서 " + recentDong.get() + "으로 생활권이 바뀌었어요");
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

        return Optional.of("최근 " + newPlaceIds.size() + "곳이나 새로운 곳을 방문했어요");
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
}
