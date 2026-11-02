package kr.chapchap.recommendation.application.port;

import java.util.List;

public interface CategoryCatalogLookupPort {

    List<String> findAllCategories();
}
