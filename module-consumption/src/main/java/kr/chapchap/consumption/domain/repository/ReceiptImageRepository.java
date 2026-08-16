package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.ReceiptImage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
