package kr.chapchap.recommendation.infra.external;

import kr.chapchap.consumption.application.service.PopularityQueryService;
import kr.chapchap.recommendation.application.port.UserTopCategoryLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RecommendationUserTopCategoryLookupAdapter implements UserTopCategoryLookupPort {

    private final PopularityQueryService popularityQueryService;

    @Override
    public Optional<String> findTopCategory(Long userId) {
        return popularityQueryService.findTopCategory(userId);
    }
}
