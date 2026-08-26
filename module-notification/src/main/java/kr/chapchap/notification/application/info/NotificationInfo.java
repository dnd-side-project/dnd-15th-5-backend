package kr.chapchap.notification.application.info;

import kr.chapchap.notification.domain.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationInfo(
        Long id,
        NotificationType type,
        String title,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
}