package kr.chapchap.consumption.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.chapchap.core.persistence.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "consumption_images")
public class ConsumptionImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsumptionImageStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "attached_at")
    private LocalDateTime attachedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private ConsumptionImage(
            Long userId,
            String objectKey,
            String contentType,
            Long fileSizeBytes,
            LocalDateTime expiresAt
    ) {
        this.userId = userId;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = ConsumptionImageStatus.TEMPORARY;
        this.expiresAt = expiresAt;
    }

    public static ConsumptionImage createTemporary(
            Long userId,
            String objectKey,
            String contentType,
            long fileSizeBytes,
            LocalDateTime expiresAt
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 식별자는 0보다 커야 합니다.");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("소비기록 이미지 Object Key는 비어 있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("소비기록 이미지 Content-Type은 비어 있을 수 없습니다.");
        }
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("소비기록 이미지 파일 크기는 0보다 커야 합니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("임시 이미지 만료 시각은 필수입니다.");
        }
        return new ConsumptionImage(
                userId,
                objectKey,
                contentType,
                fileSizeBytes,
                expiresAt
        );
    }

    public boolean isAttached() {
        return status == ConsumptionImageStatus.ATTACHED;
    }

    public boolean isExpiredAt(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("만료 여부를 확인할 시각은 필수입니다.");
        }
        return expiresAt != null && !expiresAt.isAfter(dateTime);
    }

    public void markDeleting(LocalDateTime cleanupAt) {
        if (status != ConsumptionImageStatus.TEMPORARY || isAttached()) {
            throw new IllegalStateException("임시 상태의 소비기록 이미지만 정리할 수 있습니다.");
        }
        if (!isExpiredAt(cleanupAt)) {
            throw new IllegalStateException("만료된 소비기록 이미지만 정리할 수 있습니다.");
        }

        this.status = ConsumptionImageStatus.DELETING;
    }

    public void attach(LocalDateTime attachedAt) {
        if (isAttached() || status != ConsumptionImageStatus.TEMPORARY) {
            throw new IllegalStateException("임시 상태의 소비기록 이미지만 소비 기록에 연결할 수 있습니다.");
        }
        if (attachedAt == null) {
            throw new IllegalArgumentException("소비기록 이미지 연결 시각은 필수입니다.");
        }
        if (isExpiredAt(attachedAt)) {
            throw new IllegalStateException("만료된 소비기록 이미지는 소비 기록에 연결할 수 없습니다.");
        }

        this.status = ConsumptionImageStatus.ATTACHED;
        this.attachedAt = attachedAt;
        this.expiresAt = null;
    }
}
