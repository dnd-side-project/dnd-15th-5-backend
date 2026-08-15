package kr.chapchap.place.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.NearbyPlaceInfo;
import kr.chapchap.place.application.info.PlaceLocationInfo;
import kr.chapchap.place.domain.entity.Place;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import kr.chapchap.place.domain.repository.PlaceRepository;
import kr.chapchap.place.exception.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PlaceQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceLikeRepository placeLikeRepository;
    private final EntityManager entityManager;

    public Map<Long, String> findNamesByIds(List<Long> placeIds) {
        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));
    }

    public Map<Long, String> findDongNamesByIds(List<Long> placeIds) {
        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Place::getAdministrativeDongName));
    }

    public Map<Long, PlaceLocationInfo> findLocationsByIds(List<Long> placeIds) {
        Map<Long, PlaceLocationInfo> locations = placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, PlaceLocationInfo::from));

        boolean anyMissing = placeIds.stream().distinct().anyMatch(placeId -> !locations.containsKey(placeId));
        if (anyMissing) {
            throw new BusinessException(PlaceErrorCode.LOCATION_NOT_FOUND);
        }
        return locations;
    }

    public Set<Long> findLikedPlaceIds(Long userId) {
        return placeLikeRepository.findByUserId(userId).stream()
                .map(PlaceLike::getPlaceId)
                .collect(Collectors.toSet());
    }


    @SuppressWarnings("unchecked")
    public List<NearbyPlaceInfo> findWithinRadius(double latitude, double longitude, double radiusMeters) {
        Query query = entityManager.createNativeQuery(
                "SELECT id, name, administrative_dong_name, "
                        + "ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lng "
                        + "FROM places "
                        + "WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)"
        );
        query.setParameter("lat", latitude);
        query.setParameter("lng", longitude);
        query.setParameter("radiusMeters", radiusMeters);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new NearbyPlaceInfo(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).doubleValue()))
                .toList();
    }
}
