package kr.chapchap.consumption;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.command.ReceiptOcrCommand;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.application.port.ReceiptOcrPort;
import kr.chapchap.consumption.application.service.ReceiptOcrService;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.application.port.GooglePlaceTextSearchPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReceiptOcrPersistenceIntegrationTest {

    private static final String OBJECT_KEY = "receipts/1/receipt-key";
    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    private final ReceiptOcrService receiptOcrService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReceiptOcrPort receiptOcrPort;

    @MockitoBean
    private ReceiptImageStorage receiptImageStorage;

    @MockitoBean
    private GooglePlaceTextSearchPort googlePlaceTextSearchPort;

    @Autowired
    ReceiptOcrPersistenceIntegrationTest(
            ReceiptOcrService receiptOcrService,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.receiptOcrService = receiptOcrService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM receipt_images");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void OCR과_이미지_업로드는_트랜잭션_밖에서_실행하고_이미지_정보는_DB에_저장한다() {
        // given
        User user = saveActiveUser();
        given(receiptOcrPort.recognize(any(byte[].class), eq("image/png")))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                            .isFalse();
                    return List.of(
                            "찹찹카페",
                            "주소 서울특별시 강남구 테헤란로 123",
                            "결제금액 33,000원"
                    );
                });
        given(receiptImageStorage.store(user.getId(), PNG_IMAGE, "image/png"))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                            .isFalse();
                    return OBJECT_KEY;
                });
        given(googlePlaceTextSearchPort.searchFirst(anyString()))
                .willReturn(Optional.empty());

        // when
        ReceiptOcrInfo result = receiptOcrService.recognize(
                new ReceiptOcrCommand(user.getId(), PNG_IMAGE)
        );

        // then
        Map<String, Object> receiptImage = jdbcTemplate.queryForMap(
                "SELECT * FROM receipt_images WHERE id = ?",
                result.receiptImageId()
        );
        assertThat(receiptImage.get("user_id")).isEqualTo(user.getId());
        assertThat(receiptImage.get("object_key")).isEqualTo(OBJECT_KEY);
        assertThat(receiptImage.get("content_type")).isEqualTo("image/png");
        assertThat(receiptImage.get("file_size_bytes"))
                .isEqualTo((long) PNG_IMAGE.length);
        assertThat(receiptImage.get("status")).isEqualTo("TEMPORARY");
        assertThat(receiptImage.get("expires_at")).isNotNull();
        assertThat(receiptImage.get("consumption_id")).isNull();
        assertThat(result.googlePlaceSearchResult()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM places", Long.class))
                .isZero();
    }

    @Test
    void DB_저장이_롤백되면_업로드한_영수증_이미지를_삭제한다() {
        // given
        long missingUserId = 999_999L;
        given(receiptOcrPort.recognize(any(byte[].class), eq("image/png")))
                .willReturn(List.of());
        given(receiptImageStorage.store(missingUserId, PNG_IMAGE, "image/png"))
                .willReturn(OBJECT_KEY);

        // when & then
        assertThatThrownBy(() -> receiptOcrService.recognize(
                new ReceiptOcrCommand(missingUserId, PNG_IMAGE)
        )).isInstanceOf(RuntimeException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM receipt_images",
                Long.class
        )).isZero();
        then(receiptImageStorage).should().delete(OBJECT_KEY);
    }

    private User saveActiveUser() {
        User user = User.create("찹찹이");
        user.completeTermsAgreement();
        return userRepository.save(user);
    }
}
