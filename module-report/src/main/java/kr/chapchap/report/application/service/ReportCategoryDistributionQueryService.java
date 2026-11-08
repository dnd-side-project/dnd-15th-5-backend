package kr.chapchap.report.application.service;

import kr.chapchap.report.application.info.CategoryDistributionInfo;
import kr.chapchap.report.application.info.CategoryDistributionInfo.CategoryItemInfo;
import kr.chapchap.report.application.info.ConsumptionActivity;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReportCategoryDistributionQueryService {

    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ConsumptionActivityPort consumptionActivityPort;
    private final Clock clock;

    public CategoryDistributionInfo getDistribution(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusMonths(1);
        LocalDate toExclusive = today.plusDays(1);

        List<ConsumptionActivity> activities = consumptionActivityPort.findActivities(userId, from, toExclusive);
        int totalCount = activities.size();
        if (totalCount == 0) {
            return new CategoryDistributionInfo(List.of());
        }

        Map<String, Long> countsByCategory = activities.stream().collect(Collectors.groupingBy(ConsumptionActivity::category, Collectors.counting()));

        List<CategoryItemInfo> categories = countsByCategory.entrySet().stream()
                .map(entry -> new CategoryItemInfo(
                        entry.getKey(),
                        entry.getValue(),
                        BigDecimal.valueOf(entry.getValue())
                                .multiply(HUNDRED)
                                .divide(BigDecimal.valueOf(totalCount), PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(CategoryItemInfo::percentage).reversed()
                        .thenComparing(CategoryItemInfo::category))
                .toList();

        return new CategoryDistributionInfo(categories);
    }
}
