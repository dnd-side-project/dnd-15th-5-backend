package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    long countByUserIdAndPlaceId(Long userId, Long placeId);
}
