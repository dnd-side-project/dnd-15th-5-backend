package kr.chapchap.account.infra.external.s3;

import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.infra.config.ProfileImageStorageProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class S3ProfileImageStorage implements ProfileImageStorage {

    private static final String PROFILE_IMAGE_KEY_FORMAT = "profiles/%d/%s";
    private static final String PROFILE_IMAGE_PREFIX_FORMAT = "profiles/%d/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ProfileImageStorageProperties properties;

    @Override
    public String createReadUrl(String objectKey) {
        validateObjectKey(objectKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.readUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest)
                    .url()
                    .toString();
        } catch (SdkException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

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
            throw new IllegalArgumentException("프로필 이미지 파일은 비어 있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("프로필 이미지 Content-Type은 비어 있을 수 없습니다.");
        }

        String objectKey = PROFILE_IMAGE_KEY_FORMAT.formatted(userId, UUID.randomUUID());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            return objectKey;
        } catch (SdkException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 식별자는 0보다 커야 합니다.");
        }

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(properties.bucket())
                .prefix(PROFILE_IMAGE_PREFIX_FORMAT.formatted(userId))
                .build();
        try {
            while (true) {
                ListObjectsV2Response response = s3Client.listObjectsV2(request);
                if (response.contents().isEmpty()) {
                    return;
                }
                for (S3Object object : response.contents()) {
                    delete(object.key());
                }
            }
        } catch (SdkException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("프로필 이미지 Object Key는 비어 있을 수 없습니다.");
        }
    }
}
