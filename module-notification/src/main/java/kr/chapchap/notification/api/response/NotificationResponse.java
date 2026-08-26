package kr.chapchap.notification.api.response;

import kr.chapchap.notification.application.info.NotificationInfo;
import kr.chapchap.notification.domain.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(NotificationInfo info) {
        return new NotificationResponse(
                info.id(), info.type(), info.title(), info.body(), info.read(), info.createdAt()
        );
    }
}
