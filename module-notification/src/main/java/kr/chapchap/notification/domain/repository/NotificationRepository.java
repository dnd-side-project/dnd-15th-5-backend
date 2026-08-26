package kr.chapchap.notification.domain.repository;

import kr.chapchap.notification.domain.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    List<Notification> findByUserIdOrderByIdDesc(Long userId,Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);
}
