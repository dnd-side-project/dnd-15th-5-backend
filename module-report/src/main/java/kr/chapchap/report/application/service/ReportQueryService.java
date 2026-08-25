package kr.chapchap.report.application.service;

import kr.chapchap.report.application.command.CurrentStatusCommand;
import kr.chapchap.report.application.info.AcquiredSticker;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.info.CurrentStatusInfo;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.DongNameLookupPort;
import kr.chapchap.report.application.port.MonthlyStickerLookupPort;
import kr.chapchap.report.domain.entity.VisitActivity;
import kr.chapchap.report.domain.service.RecentDiscoveryMessageGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReportQueryService {

    private static final int DAYS_IN_WEEK = 7;
    private static final int RECENT_WINDOW_DAYS = 14;
    private static final int TREND_LOOKBACK_DAYS = RECENT_WINDOW_DAYS * 2;

    private final ConsumptionActivityPort consumptionActivityPort;
    private final DongNameLookupPort dongNameLookupPort;
    private final MonthlyStickerLookupPort monthlyStickerLookupPort;
    private final RecentDiscoveryMessageGenerator recentDiscoveryMessageGenerator;
    private final Clock clock;

    public CurrentStatusInfo getCurrentStatus(CurrentStatusCommand command) {
        LocalDate today = LocalDate.now(clock);
        YearMonth yearMonth = command.yearMonth();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() % DAYS_IN_WEEK);
        LocalDate trendStart = today.minusDays(TREND_LOOKBACK_DAYS - 1L);

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEndInclusive = yearMonth.equals(YearMonth.from(today)) ? today : yearMonth.atEndOfMonth();

        LocalDate fetchStart = earliestOf(monthStart, weekStart, trendStart);
        LocalDate fetchEndInclusive = monthEndInclusive.isAfter(today) ? monthEndInclusive : today;

        List<ConsumptionActivity> activities =
                consumptionActivityPort.findActivities(command.userId(), fetchStart, fetchEndInclusive.plusDays(1));

        List<Integer> weeklyCounts = calculateWeeklyCounts(activities, weekStart);
        int monthlyCount = calculateMonthlyCount(activities, monthStart, monthEndInclusive);
        Map<String, Integer> monthlyCategoryCounts = calculateMonthlyCategoryCounts(activities, monthStart, monthEndInclusive);
        String recentDiscoveryMessage = buildRecentDiscoveryMessage(activities, today, trendStart);
        List<AcquiredSticker> monthlyStickers = monthlyStickerLookupPort.findRecentAcquiredStickers(
                command.userId(), monthStart, monthEndInclusive.plusDays(1));

        return new CurrentStatusInfo(
                today, weeklyCounts, monthlyCount, monthlyCategoryCounts, recentDiscoveryMessage, monthlyStickers);
    }

    // 일(0) ~ 토(6) 순서로 반환
    private List<Integer> calculateWeeklyCounts(List<ConsumptionActivity> activities, LocalDate weekStart) {
        LocalDate weekEndExclusive = weekStart.plusDays(DAYS_IN_WEEK);
        int[] counts = new int[DAYS_IN_WEEK];
        for (ConsumptionActivity activity : activities) {
            LocalDate date = activity.purchaseDate();
            if (!date.isBefore(weekStart) && date.isBefore(weekEndExclusive)) {
                counts[date.getDayOfWeek().getValue() % DAYS_IN_WEEK]++;
            }
        }
        return Arrays.stream(counts).boxed().toList();
    }

    private int calculateMonthlyCount(List<ConsumptionActivity> activities, LocalDate monthStart, LocalDate monthEndInclusive) {
        return (int) activities.stream()
                .filter(activity -> !activity.purchaseDate().isBefore(monthStart)
                        && !activity.purchaseDate().isAfter(monthEndInclusive))
                .count();
    }


    private Map<String, Integer> calculateMonthlyCategoryCounts(List<ConsumptionActivity> activities, LocalDate monthStart, LocalDate monthEndInclusive) {
        return activities.stream()
                .filter(activity -> !activity.purchaseDate().isBefore(monthStart)
                        && !activity.purchaseDate().isAfter(monthEndInclusive))
                .collect(Collectors.groupingBy(ConsumptionActivity::category, Collectors.summingInt(activity -> 1)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private String buildRecentDiscoveryMessage(List<ConsumptionActivity> activities, LocalDate today, LocalDate trendStart) {
        LocalDate recentStart = today.minusDays(RECENT_WINDOW_DAYS - 1L);

        List<ConsumptionActivity> trendActivities = activities.stream()
                .filter(activity -> !activity.purchaseDate().isBefore(trendStart) && !activity.purchaseDate().isAfter(today))
                .toList();

        List<Long> placeIds = trendActivities.stream().map(ConsumptionActivity::placeId).distinct().toList();
        Map<Long, String> dongNames = dongNameLookupPort.findDongNames(placeIds);

        List<VisitActivity> recent = trendActivities.stream()
                .filter(activity -> !activity.purchaseDate().isBefore(recentStart))
                .map(activity -> toVisitActivity(activity, dongNames))
                .toList();

        List<VisitActivity> previous = trendActivities.stream()
                .filter(activity -> activity.purchaseDate().isBefore(recentStart))
                .map(activity -> toVisitActivity(activity, dongNames))
                .toList();

        return recentDiscoveryMessageGenerator.generate(recent, previous);
    }

    private VisitActivity toVisitActivity(ConsumptionActivity activity, Map<Long, String> dongNames) {
        return new VisitActivity(
                activity.placeId(),
                dongNames.get(activity.placeId()),
                activity.category(),
                activity.purchaseDate(),
                activity.purchaseTime()
        );
    }

    private LocalDate earliestOf(LocalDate a, LocalDate b, LocalDate c) {
        LocalDate min = a.isBefore(b) ? a : b;
        return min.isBefore(c) ? min : c;
    }
}
