package kr.chapchap.place.domain.repository;

import kr.chapchap.place.domain.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
