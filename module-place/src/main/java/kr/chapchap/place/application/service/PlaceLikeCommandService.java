package kr.chapchap.place.application.service;

import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaceLikeCommandService {

    private final PlaceLikeRepository placeLikeRepository;

    public boolean toggle(Long userId, Long placeId) {
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
