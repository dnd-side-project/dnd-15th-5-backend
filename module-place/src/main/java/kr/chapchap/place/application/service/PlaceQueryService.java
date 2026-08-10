package kr.chapchap.place.application.service;

import kr.chapchap.place.domain.entity.Place;
import kr.chapchap.place.domain.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PlaceQueryService {

    private final PlaceRepository placeRepository;

    public Map<Long, String> findNamesByIds(List<Long> placeIds) {
        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));
    }

    public Map<Long, String> findDongNamesByIds(List<Long> placeIds) {
        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Place::getAdministrativeDongName));
    }
}
