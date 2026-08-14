package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
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

    public Set<Long> findLikedPlaceIds(Long userId, List<Long> placeIds) {
        return placeLikeRepository.findByUserIdAndPlaceIdIn(userId, placeIds).stream()
                .map(PlaceLike::getPlaceId)
                .collect(Collectors.toSet());
    }
}
