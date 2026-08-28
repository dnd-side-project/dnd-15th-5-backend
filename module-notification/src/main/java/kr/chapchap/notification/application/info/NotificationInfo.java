package kr.chapchap.notification.application.info;

import java.time.LocalDateTime;

public record NotificationInfo(
        Long id,
        String type,
        String title,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
}
