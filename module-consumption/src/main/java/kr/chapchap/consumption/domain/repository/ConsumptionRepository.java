package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.Consumption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    Optional<Consumption> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Consumption c WHERE c.id = :id AND c.userId = :userId")
    Optional<Consumption> findByIdAndUserIdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    long countByUserIdAndPlaceId(Long userId, Long placeId);
}
