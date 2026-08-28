package kr.chapchap.notification.domain.repository;

import kr.chapchap.notification.domain.entity.Notification;
import kr.chapchap.notification.domain.entity.NotificationType;
import kr.chapchap.notification.domain.entity.PushStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndCreatedAtAfterOrderByIdDesc(
            Long userId, LocalDateTime createdAtAfter, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndReadFalse(Long userId);

    List<Notification> findByTypeAndPushStatus(NotificationType type, PushStatus pushStatus);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.userId = :userId AND n.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
