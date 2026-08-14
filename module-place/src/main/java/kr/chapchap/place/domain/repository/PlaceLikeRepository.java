package kr.chapchap.place.domain.repository;

import kr.chapchap.place.domain.entity.PlaceLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceLikeRepository extends JpaRepository<PlaceLike, Long> {

    Optional<PlaceLike> findByUserIdAndPlaceId(Long userId, Long placeId);

    List<PlaceLike> findByUserId(Long userId);
}
