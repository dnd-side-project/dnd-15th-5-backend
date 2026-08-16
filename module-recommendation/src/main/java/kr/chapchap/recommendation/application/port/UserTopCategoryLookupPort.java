package kr.chapchap.recommendation.application.port;

import java.util.Optional;

public interface UserTopCategoryLookupPort {

    Optional<String> findTopCategory(Long userId);
}
