package kr.chapchap.consumption.infra.external.s3;

import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.infra.config.ReceiptImageStorageProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class S3ReceiptImageStorage implements ReceiptImageStorage {

    private static final String RECEIPT_IMAGE_KEY_FORMAT = "receipts/%d/%s";

    private final S3Client s3Client;
    private final ReceiptImageStorageProperties properties;

    @Override
    public String store(
            Long userId,
            byte[] content,
            String contentType
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 식별자는 0보다 커야 합니다.");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("영수증 이미지 파일은 비어 있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("영수증 이미지 Content-Type은 비어 있을 수 없습니다.");
        }

        String objectKey = RECEIPT_IMAGE_KEY_FORMAT.formatted(
                userId,
                UUID.randomUUID()
        );
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
            return objectKey;
        } catch (SdkException exception) {
            throw new BusinessException(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    exception
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("영수증 이미지 Object Key는 비어 있을 수 없습니다.");
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            throw new BusinessException(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    exception
            );
        }
    }
}
