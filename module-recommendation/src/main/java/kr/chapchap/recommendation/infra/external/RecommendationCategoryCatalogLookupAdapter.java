package kr.chapchap.recommendation.infra.external;

import kr.chapchap.consumption.application.service.StickerQueryService;
import kr.chapchap.recommendation.application.port.CategoryCatalogLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RecommendationCategoryCatalogLookupAdapter implements CategoryCatalogLookupPort {

    private final StickerQueryService stickerQueryService;

    @Override
    public List<String> findAllCategories() {
        return stickerQueryService.findAllCategoriesInCatalogOrder();
    }
}
