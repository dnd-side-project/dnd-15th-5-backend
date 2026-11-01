package kr.chapchap.place.application.service;

import jakarta.persistence.EntityManager;
import kr.chapchap.place.application.info.TownSearchResultInfo;
import kr.chapchap.place.domain.entity.AdministrativeDong;
import kr.chapchap.place.domain.repository.AdministrativeDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TownSearchQueryService {

    private static final int DEFAULT_RESULT_LIMIT = 10;
    private static final double FUZZY_MATCH_THRESHOLD = 0.3;

    private final AdministrativeDongRepository administrativeDongRepository;
    private final EntityManager entityManager;

    public List<TownSearchResultInfo> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String trimmed = keyword.trim();

        List<AdministrativeDong> exactMatches =
                administrativeDongRepository.searchByExactMatch(trimmed, DEFAULT_RESULT_LIMIT);
        List<AdministrativeDong> results = exactMatches.isEmpty()
                ? searchByFuzzyMatch(trimmed)
                : exactMatches;

        return results.stream().map(TownSearchResultInfo::from).toList();
    }

    private List<AdministrativeDong> searchByFuzzyMatch(String trimmed) {
        entityManager.createNativeQuery("SET LOCAL pg_trgm.word_similarity_threshold = " + FUZZY_MATCH_THRESHOLD)
                .executeUpdate();
        return administrativeDongRepository.searchByFuzzyMatch(trimmed, DEFAULT_RESULT_LIMIT);
    }
}
