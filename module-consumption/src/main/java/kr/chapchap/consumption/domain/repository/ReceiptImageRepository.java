package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
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

public interface ReceiptImageRepository extends JpaRepository<ReceiptImage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT receiptImage
            FROM ReceiptImage receiptImage
            WHERE receiptImage.id = :id
              AND receiptImage.userId = :userId
            """)
    Optional<ReceiptImage> findByIdAndUserIdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT receiptImage.id
            FROM ReceiptImage receiptImage
            WHERE receiptImage.id > :afterId
              AND (
                    (receiptImage.status = :temporaryStatus AND receiptImage.expiresAt <= :expiredAt)
                    OR receiptImage.status = :deletingStatus
              )
            ORDER BY receiptImage.id
            """)
    List<Long> findCleanupCandidateIds(
            @Param("afterId") Long afterId,
            @Param("expiredAt") LocalDateTime expiredAt,
            @Param("temporaryStatus") ReceiptImageStatus temporaryStatus,
            @Param("deletingStatus") ReceiptImageStatus deletingStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT receiptImage
            FROM ReceiptImage receiptImage
            WHERE receiptImage.id = :id
            """)
    Optional<ReceiptImage> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            DELETE FROM ReceiptImage receiptImage
            WHERE receiptImage.id = :id
              AND receiptImage.status = :status
            """)
    int deleteByIdAndStatus(
            @Param("id") Long id,
            @Param("status") ReceiptImageStatus status
    );
}
