package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import kr.chapchap.place.domain.repository.PlaceRepository;
import kr.chapchap.place.exception.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaceLikeCommandService {

    private final PlaceLikeRepository placeLikeRepository;
    private final PlaceRepository placeRepository;

    public boolean toggle(Long userId, Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        return placeLikeRepository.findByUserIdAndPlaceId(userId, placeId)
                .map(placeLike -> {
                    placeLikeRepository.delete(placeLike);
                    return false;
                })
                .orElseGet(() -> {
                    placeLikeRepository.save(PlaceLike.builder()
                            .userId(userId)
                            .placeId(placeId)
                            .build());
                    return true;
                });
    }
}
