package kr.chapchap.consumption;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.application.service.ReceiptImageCleanupService;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReceiptImageCleanupPersistenceIntegrationTest {

    private static final String EXPIRED_OBJECT_KEY = "receipts/1/expired-receipt";
    private static final String FUTURE_OBJECT_KEY = "receipts/1/future-receipt";

    private final ReceiptImageCleanupService receiptImageCleanupService;
    private final ReceiptImageRepository receiptImageRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReceiptImageStorage receiptImageStorage;

    @Autowired
    ReceiptImageCleanupPersistenceIntegrationTest(
            ReceiptImageCleanupService receiptImageCleanupService,
            ReceiptImageRepository receiptImageRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.receiptImageCleanupService = receiptImageCleanupService;
        this.receiptImageRepository = receiptImageRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM receipt_images");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 만료된_이미지는_트랜잭션_밖에서_S3에서_삭제한_뒤_DB에서_삭제한다() {
        // given
        User user = saveActiveUser();
        ReceiptImage expiredImage = saveTemporaryImage(
                user.getId(),
                EXPIRED_OBJECT_KEY,
                LocalDateTime.now().minusHours(1)
        );
        ReceiptImage futureImage = saveTemporaryImage(
                user.getId(),
                FUTURE_OBJECT_KEY,
                LocalDateTime.now().plusHours(1)
        );
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return null;
        }).given(receiptImageStorage).delete(EXPIRED_OBJECT_KEY);

        // when
        int deletedCount = receiptImageCleanupService.cleanupExpiredImages();

        // then
        assertThat(deletedCount).isOne();
        assertThat(receiptImageRepository.findById(expiredImage.getId())).isEmpty();
        assertThat(receiptImageRepository.findById(futureImage.getId()))
                .get()
                .extracting(ReceiptImage::getStatus)
                .isEqualTo(ReceiptImageStatus.TEMPORARY);
        then(receiptImageStorage).should().delete(EXPIRED_OBJECT_KEY);
    }

    @Test
    void S3_삭제에_실패한_이미지는_정리_중_상태로_남겨_다음_실행에서_재시도한다() {
        // given
        User user = saveActiveUser();
        ReceiptImage expiredImage = saveTemporaryImage(
                user.getId(),
                EXPIRED_OBJECT_KEY,
                LocalDateTime.now().minusHours(1)
        );
        willThrow(new IllegalStateException("S3 일시 오류"))
                .willDoNothing()
                .given(receiptImageStorage)
                .delete(EXPIRED_OBJECT_KEY);

        // when
        int firstDeletedCount = receiptImageCleanupService.cleanupExpiredImages();

        // then
        assertThat(firstDeletedCount).isZero();
        assertThat(receiptImageRepository.findById(expiredImage.getId()))
                .get()
                .extracting(ReceiptImage::getStatus)
                .isEqualTo(ReceiptImageStatus.DELETING);

        // when
        int retryDeletedCount = receiptImageCleanupService.cleanupExpiredImages();

        // then
        assertThat(retryDeletedCount).isOne();
        assertThat(receiptImageRepository.findById(expiredImage.getId())).isEmpty();
        then(receiptImageStorage).should(times(2)).delete(EXPIRED_OBJECT_KEY);
    }

    private ReceiptImage saveTemporaryImage(
            Long userId,
            String objectKey,
            LocalDateTime expiresAt
    ) {
        return receiptImageRepository.save(ReceiptImage.createTemporary(
                userId,
                objectKey,
                "image/png",
                100L,
                expiresAt
        ));
    }

    private User saveActiveUser() {
        User user = User.create("찹찹이");
        user.completeTermsAgreement();
        return userRepository.save(user);
    }
}
