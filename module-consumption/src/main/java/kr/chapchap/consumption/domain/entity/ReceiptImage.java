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
@Table(name = "receipt_images")
public class ReceiptImage extends BaseTimeEntity {

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
    private ReceiptImageStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "attached_at")
    private LocalDateTime attachedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "consumption_id")
    private Long consumptionId;

    private ReceiptImage(
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
        this.status = ReceiptImageStatus.TEMPORARY;
        this.expiresAt = expiresAt;
    }

    public static ReceiptImage createTemporary(
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
            throw new IllegalArgumentException("영수증 이미지 Object Key는 비어 있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("영수증 이미지 Content-Type은 비어 있을 수 없습니다.");
        }
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("영수증 이미지 파일 크기는 0보다 커야 합니다.");
        }
        return new ReceiptImage(
                userId,
                objectKey,
                contentType,
                fileSizeBytes,
                expiresAt
        );
    }
}
