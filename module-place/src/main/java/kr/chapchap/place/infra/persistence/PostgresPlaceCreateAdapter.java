package kr.chapchap.place.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.port.PlaceCreatePort;
import kr.chapchap.place.domain.entity.Place;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class PostgresPlaceCreateAdapter implements PlaceCreatePort {

    private static final String INSERT_IF_ABSENT_SQL = """
            INSERT INTO places (
                google_place_id,
                name,
                road_address,
                administrative_dong_code,
                administrative_dong_name,
                location,
                created_at,
                updated_at
            ) VALUES (
                :googlePlaceId,
                :name,
                :roadAddress,
                :administrativeDongCode,
                :administrativeDongName,
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (google_place_id) DO NOTHING
            RETURNING id
            """;
    private static final String FIND_ID_SQL = """
            SELECT id
            FROM places
            WHERE google_place_id = :googlePlaceId
            """;

    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createOrGet(Place place) {
        Query insertQuery = entityManager.createNativeQuery(INSERT_IF_ABSENT_SQL);
        setInsertParameters(insertQuery, place);

        List<?> insertedIds = insertQuery.getResultList();
        if (!insertedIds.isEmpty()) {
            return ((Number) insertedIds.getFirst()).longValue();
        }

        Query findQuery = entityManager.createNativeQuery(FIND_ID_SQL);
        findQuery.setParameter("googlePlaceId", place.getGooglePlaceId());
        List<?> existingIds = findQuery.getResultList();
        if (existingIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        return ((Number) existingIds.getFirst()).longValue();
    }

    private void setInsertParameters(Query query, Place place) {
        query.setParameter("googlePlaceId", place.getGooglePlaceId());
        query.setParameter("name", place.getName());
        query.setParameter("roadAddress", place.getRoadAddress());
        query.setParameter("administrativeDongCode", place.getAdministrativeDongCode());
        query.setParameter("administrativeDongName", place.getAdministrativeDongName());
        query.setParameter("longitude", place.getLongitude());
        query.setParameter("latitude", place.getLatitude());
    }
}
