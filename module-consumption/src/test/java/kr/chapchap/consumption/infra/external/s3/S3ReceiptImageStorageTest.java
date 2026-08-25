package kr.chapchap.consumption.infra.external.s3;

import kr.chapchap.consumption.infra.config.ReceiptImageStorageProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
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
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class S3ReceiptImageStorageTest {

    private static final String BUCKET = "chapchap-receipt-images";
    private static final String OBJECT_KEY = "receipts/1/0c620c8f-f5c5-4daf-8204-1bb06d8d29f2";
    private static final String ANOTHER_OBJECT_KEY = "receipts/1/another-image";

    @Mock
    private S3Client s3Client;

    private S3ReceiptImageStorage receiptImageStorage;

    @BeforeEach
    void setUp() {
        receiptImageStorage = new S3ReceiptImageStorage(
                s3Client,
                new ReceiptImageStorageProperties(BUCKET)
        );
    }

    @Test
    void 사용자별_Key로_영수증_이미지를_저장한다() throws Exception {
        // given
        byte[] content = new byte[]{1, 2, 3};
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        String result = receiptImageStorage.store(1L, content, "image/png");

        // then
        assertThat(result).matches("receipts/1/[0-9a-f-]{36}");
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
    void 저장된_Key의_영수증_이미지를_삭제한다() {
        // given
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(DeleteObjectResponse.builder().build());

        // when
        receiptImageStorage.delete(OBJECT_KEY);

        // then
        ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        then(s3Client).should().deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(OBJECT_KEY);
    }

    @Test
    void 사용자_Prefix의_모든_영수증_이미지를_삭제한다() {
        // given
        given(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .willReturn(
                        ListObjectsV2Response.builder()
                                .contents(
                                        S3Object.builder().key(OBJECT_KEY).build(),
                                        S3Object.builder().key(ANOTHER_OBJECT_KEY).build()
                                )
                                .build(),
                        ListObjectsV2Response.builder().build()
                );
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(DeleteObjectResponse.builder().build());

        // when
        receiptImageStorage.deleteAllByUserId(1L);

        // then
        ArgumentCaptor<ListObjectsV2Request> listRequestCaptor =
                ArgumentCaptor.forClass(ListObjectsV2Request.class);
        then(s3Client).should(times(2)).listObjectsV2(listRequestCaptor.capture());
        assertThat(listRequestCaptor.getAllValues()).allSatisfy(request -> {
            assertThat(request.bucket()).isEqualTo(BUCKET);
            assertThat(request.prefix()).isEqualTo("receipts/1/");
        });
        ArgumentCaptor<DeleteObjectRequest> deleteRequestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        then(s3Client).should(times(2)).deleteObject(deleteRequestCaptor.capture());
        assertThat(deleteRequestCaptor.getAllValues())
                .extracting(DeleteObjectRequest::key)
                .containsExactly(OBJECT_KEY, ANOTHER_OBJECT_KEY);
    }

    @Test
    void 사용자_영수증_이미지_목록_조회에_실패하면_외부_서비스_오류로_변환한다() {
        // given
        SdkClientException cause = SdkClientException.create("S3 unavailable");
        given(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .willThrow(cause);

        // when & then
        assertThatThrownBy(() -> receiptImageStorage.deleteAllByUserId(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

    @Test
    void S3_저장에_실패하면_외부_서비스_오류로_변환한다() {
        // given
        SdkClientException cause = SdkClientException.create("S3 unavailable");
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(cause);

        // when & then
        assertThatThrownBy(() -> receiptImageStorage.store(
                1L,
                new byte[]{1},
                "image/png"
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(
                    CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
            );
            assertThat(exception.getCause()).isSameAs(cause);
        });
    }
}
