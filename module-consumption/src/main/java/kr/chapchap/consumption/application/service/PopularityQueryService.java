package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlacePopularityInfo;
import kr.chapchap.consumption.domain.entity.CategoryCountRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// module-recommendation이 크로스모듈로 조회하는 공개 API.
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PopularityQueryService {

    private static final int TOP_CATEGORY_WINDOW_DAYS = 30;

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final Clock clock;

    //반경 내 인기
    public List<PlacePopularityInfo> aggregatePopularityByPlaceIds(List<Long> placeIds) {
        return consumptionQueryRepository.aggregatePopularityByPlaceIds(placeIds).stream()
                .map(row -> new PlacePopularityInfo(row.placeId(), row.category(), row.visitCount(), row.lastVisitedDate()))
                .toList();
    }

    // module-recommendation의 가게 추천에서 이미 방문한 장소 제외용
    public Set<Long> findVisitedPlaceIds(Long userId) {
        return Set.copyOf(consumptionQueryRepository.findDistinctVisitedPlaceIds(userId));
    }

    public Optional<String> findTopCategory(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(TOP_CATEGORY_WINDOW_DAYS);
        LocalDate toExclusive = today.plusDays(1);

        List<CategoryCountRow> rows = consumptionQueryRepository.aggregateCategoryCounts(userId, from, toExclusive);

        return rows.stream()
                .max(Comparator.comparingLong(CategoryCountRow::count))
                .map(CategoryCountRow::category);
    }
}
