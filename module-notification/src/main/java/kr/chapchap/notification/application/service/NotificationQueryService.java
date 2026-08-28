package kr.chapchap.notification.application.service;

import kr.chapchap.notification.application.info.NotificationInfo;
import kr.chapchap.notification.domain.entity.Notification;
import kr.chapchap.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationQueryService {

    private static final int RETENTION_DAYS = 30;
    private static final int MAX_RESULTS = 500;

    private final NotificationRepository notificationRepository;

    public List<NotificationInfo> getNotifications(Long userId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        List<Notification> notifications = notificationRepository
                .findByUserIdAndCreatedAtAfterOrderByIdDesc(userId, cutoff, PageRequest.of(0, MAX_RESULTS));

        return notifications.stream().map(this::toInfo).toList();
    }

    public boolean hasUnread(Long userId) {
        return notificationRepository.existsByUserIdAndReadFalse(userId);
    }

    private NotificationInfo toInfo(Notification notification) {
        return new NotificationInfo(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
