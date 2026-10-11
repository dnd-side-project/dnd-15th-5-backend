package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.ConsumptionImageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsumptionImageRepository extends JpaRepository<ConsumptionImage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT consumptionImage
            FROM ConsumptionImage consumptionImage
            WHERE consumptionImage.id = :id
              AND consumptionImage.userId = :userId
            """)
    Optional<ConsumptionImage> findByIdAndUserIdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT consumptionImage.id
            FROM ConsumptionImage consumptionImage
            WHERE consumptionImage.id > :afterId
              AND (
                    (consumptionImage.status = :temporaryStatus AND consumptionImage.expiresAt <= :expiredAt)
                    OR consumptionImage.status = :deletingStatus
              )
            ORDER BY consumptionImage.id
            """)
    List<Long> findCleanupCandidateIds(
            @Param("afterId") Long afterId,
            @Param("expiredAt") LocalDateTime expiredAt,
            @Param("temporaryStatus") ConsumptionImageStatus temporaryStatus,
            @Param("deletingStatus") ConsumptionImageStatus deletingStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT consumptionImage
            FROM ConsumptionImage consumptionImage
            WHERE consumptionImage.id = :id
            """)
    Optional<ConsumptionImage> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            DELETE FROM ConsumptionImage consumptionImage
            WHERE consumptionImage.id = :id
              AND consumptionImage.status = :status
            """)
    int deleteByIdAndStatus(
            @Param("id") Long id,
            @Param("status") ConsumptionImageStatus status
    );
}
