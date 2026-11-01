package kr.chapchap.place.domain.repository;

import kr.chapchap.place.domain.entity.AdministrativeDong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdministrativeDongRepository
        extends JpaRepository<AdministrativeDong, String>, AdministrativeDongRepositoryCustom {

    @Query(
            value = "SELECT * FROM administrative_dongs "
                    + "WHERE :keyword <% search_text "
                    + "ORDER BY word_similarity(:keyword, search_text) DESC, full_name ASC "
                    + "LIMIT :limit",
            nativeQuery = true
    )
    List<AdministrativeDong> searchByFuzzyMatch(@Param("keyword") String keyword, @Param("limit") int limit);
}
