package kr.chapchap.account.infra.external.s3;

import kr.chapchap.account.infra.config.ProfileImageStorageProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class S3ProfileImageStorageTest {

    private static final String BUCKET = "chapchap-profile-images";
    private static final String OBJECT_KEY = "profiles/1/0c620c8f-f5c5-4daf-8204-1bb06d8d29f2";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3ProfileImageStorage profileImageStorage;

    @BeforeEach
    void setUp() {
        profileImageStorage = new S3ProfileImageStorage(
                s3Client,
                s3Presigner,
                new ProfileImageStorageProperties(BUCKET, Duration.ofMinutes(2))
        );
    }

    @Test
    void 사용자별_Key로_프로필_이미지를_저장한다() throws Exception {
        // given
        byte[] content = new byte[]{1, 2, 3};
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        String result = profileImageStorage.store(1L, content, "image/png");

        // then
        assertThat(result).matches("profiles/1/[0-9a-f-]{36}");
        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        then(s3Client).should().putObject(requestCaptor.capture(), bodyCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(result);
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(bodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes())
                .isEqualTo(content);
    }

    @Test
    void 저장된_Key로_프로필_이미지_조회_URL을_생성한다() throws Exception {
        // given
        PresignedGetObjectRequest presignedRequest =
                org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(
                URI.create("https://example.com/profile-image").toURL()
        );
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willReturn(presignedRequest);

        // when
        String result = profileImageStorage.createReadUrl(OBJECT_KEY);

        // then
        assertThat(result).isEqualTo("https://example.com/profile-image");
        ArgumentCaptor<GetObjectPresignRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        then(s3Presigner).should().presignGetObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(2));
        assertThat(requestCaptor.getValue().getObjectRequest().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().getObjectRequest().key()).isEqualTo(OBJECT_KEY);
    }

    @Test
    void 저장된_Key의_프로필_이미지를_삭제한다() {
        // given
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(DeleteObjectResponse.builder().build());

        // when
        profileImageStorage.delete(OBJECT_KEY);

        // then
        ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        then(s3Client).should().deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(OBJECT_KEY);
    }

    @Test
    void S3_저장에_실패하면_외부_서비스_오류로_변환한다() {
        // given
        SdkClientException cause = SdkClientException.create("S3 unavailable");
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(cause);

        // when & then
        assertThatThrownBy(() -> profileImageStorage.store(1L, new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }
}
