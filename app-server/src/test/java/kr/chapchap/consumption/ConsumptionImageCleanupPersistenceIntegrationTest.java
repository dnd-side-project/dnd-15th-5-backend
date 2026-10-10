package kr.chapchap.consumption;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.service.ConsumptionImageCleanupService;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.ConsumptionImageStatus;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
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
class ConsumptionImageCleanupPersistenceIntegrationTest {

    private static final String EXPIRED_OBJECT_KEY = "consumption-images/1/expired-receipt";
    private static final String FUTURE_OBJECT_KEY = "consumption-images/1/future-receipt";

    private final ConsumptionImageCleanupService consumptionImageCleanupService;
    private final ConsumptionImageRepository consumptionImageRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ConsumptionImageStorage consumptionImageStorage;

    @Autowired
    ConsumptionImageCleanupPersistenceIntegrationTest(
            ConsumptionImageCleanupService consumptionImageCleanupService,
            ConsumptionImageRepository consumptionImageRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.consumptionImageCleanupService = consumptionImageCleanupService;
        this.consumptionImageRepository = consumptionImageRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM consumption_images");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 만료된_이미지는_트랜잭션_밖에서_S3에서_삭제한_뒤_DB에서_삭제한다() {
        // given
        User user = saveActiveUser();
        ConsumptionImage expiredImage = saveTemporaryImage(
                user.getId(),
                EXPIRED_OBJECT_KEY,
                LocalDateTime.now().minusHours(1)
        );
        ConsumptionImage futureImage = saveTemporaryImage(
                user.getId(),
                FUTURE_OBJECT_KEY,
                LocalDateTime.now().plusHours(1)
        );
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return null;
        }).given(consumptionImageStorage).delete(EXPIRED_OBJECT_KEY);

        // when
        int deletedCount = consumptionImageCleanupService.cleanupExpiredImages();

        // then
        assertThat(deletedCount).isOne();
        assertThat(consumptionImageRepository.findById(expiredImage.getId())).isEmpty();
        assertThat(consumptionImageRepository.findById(futureImage.getId()))
                .get()
                .extracting(ConsumptionImage::getStatus)
                .isEqualTo(ConsumptionImageStatus.TEMPORARY);
        then(consumptionImageStorage).should().delete(EXPIRED_OBJECT_KEY);
    }

    @Test
    void S3_삭제에_실패한_이미지는_정리_중_상태로_남겨_다음_실행에서_재시도한다() {
        // given
        User user = saveActiveUser();
        ConsumptionImage expiredImage = saveTemporaryImage(
                user.getId(),
                EXPIRED_OBJECT_KEY,
                LocalDateTime.now().minusHours(1)
        );
        willThrow(new IllegalStateException("S3 일시 오류"))
                .willDoNothing()
                .given(consumptionImageStorage)
                .delete(EXPIRED_OBJECT_KEY);

        // when
        int firstDeletedCount = consumptionImageCleanupService.cleanupExpiredImages();

        // then
        assertThat(firstDeletedCount).isZero();
        assertThat(consumptionImageRepository.findById(expiredImage.getId()))
                .get()
                .extracting(ConsumptionImage::getStatus)
                .isEqualTo(ConsumptionImageStatus.DELETING);

        // when
        int retryDeletedCount = consumptionImageCleanupService.cleanupExpiredImages();

        // then
        assertThat(retryDeletedCount).isOne();
        assertThat(consumptionImageRepository.findById(expiredImage.getId())).isEmpty();
        then(consumptionImageStorage).should(times(2)).delete(EXPIRED_OBJECT_KEY);
    }

    private ConsumptionImage saveTemporaryImage(
            Long userId,
            String objectKey,
            LocalDateTime expiresAt
    ) {
        return consumptionImageRepository.save(ConsumptionImage.createTemporary(
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
