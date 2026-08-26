package kr.chapchap.notification.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false, length = 20)
    private PushStatus pushStatus;

    @Column(name = "fcm_message_id", length = 255)
    private String fcmMessageId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(Long userId, NotificationType type, String title, String body) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.pushStatus = PushStatus.PENDING;
    }

    public void markAsRead(LocalDateTime now) {
        if (this.read) {
            return;
        }
        this.read = true;
        this.readAt = now;
    }

    public void markPushSent(String fcmMessageId) {
        this.pushStatus = PushStatus.SENT;
        this.fcmMessageId = fcmMessageId;
    }

    public void markPushFailed() {
        this.pushStatus = PushStatus.FAILED;
    }

    public void markPushSkipped() {
        this.pushStatus = PushStatus.SKIPPED;
    }
}
