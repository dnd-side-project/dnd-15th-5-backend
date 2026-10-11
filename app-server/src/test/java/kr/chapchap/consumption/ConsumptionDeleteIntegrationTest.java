package kr.chapchap.consumption;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.command.ConsumptionSaveCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.application.service.ConsumptionCommandService;
import kr.chapchap.consumption.application.service.ConsumptionDeleteService;
import kr.chapchap.consumption.application.service.ConsumptionImageCleanupService;
import kr.chapchap.consumption.application.service.ReceiptImageCleanupService;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.ConsumptionImageStatus;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsumptionDeleteIntegrationTest {

    @Autowired private ConsumptionCommandService commands;
    @Autowired private ConsumptionDeleteService deletion;
    @Autowired private ConsumptionImageCleanupService imageCleanup;
    @Autowired private ReceiptImageCleanupService receiptCleanup;
    @Autowired private ConsumptionRepository consumptions;
    @Autowired private ConsumptionImageRepository images;
    @Autowired private ReceiptImageRepository receipts;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private ConsumptionImageStorage imageStorage;
    @MockitoBean private ReceiptImageStorage receiptStorage;

    private Long userId;
    private Long placeId;
    private Long stickerId;

    @BeforeEach
    void setUp() {
        User user = User.create("삭제테스트");
        user.completeTermsAgreement();
        userId = users.save(user).getId();
        placeId = jdbc.queryForObject("""
                INSERT INTO places (google_place_id, name, road_address, administrative_dong_code,
                    administrative_dong_name, location)
                VALUES (?, '테스트카페', '서울특별시 강남구', '11680650', '역삼1동',
                    ST_SetSRID(ST_MakePoint(127.024551, 37.506481), 4326)::geography) RETURNING id
                """, Long.class, UUID.randomUUID().toString());
        stickerId = jdbc.queryForObject("SELECT id FROM sticker_item WHERE category = '카페' AND name = '커피'", Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM receipt_images");
        jdbc.update("DELETE FROM consumptions");
        jdbc.update("DELETE FROM consumption_images");
        jdbc.update("DELETE FROM place_likes");
        jdbc.update("DELETE FROM places");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void 방문목록의_ID로_삭제하면_다른_기록과_장소와_좋아요와_스티커는_유지한다() throws Exception {
        // given
        Long consumptionId = create(userId, null, null);
        Long otherUserId = users.save(User.create("다른회원")).getId();
        Long otherConsumptionId = create(otherUserId, null, null);
        jdbc.update("INSERT INTO place_likes (user_id, place_id) VALUES (?, ?)", userId, placeId);

        // when & then
        mvc.perform(get("/v1/consumptions/places/{placeId}/visits", placeId)
                        .with(jwt().jwt(token -> token.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visits[0].consumptionId").value(consumptionId));
        mvc.perform(delete("/v1/consumptions/{consumptionId}", consumptionId)
                        .with(jwt().jwt(token -> token.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"));
        assertThat(consumptions.existsById(consumptionId)).isFalse();
        assertThat(consumptions.existsById(otherConsumptionId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM places WHERE id = ?", Long.class, placeId)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM place_likes WHERE user_id = ?", Long.class, userId)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sticker_item WHERE id = ?", Long.class, stickerId)).isOne();
        then(imageStorage).shouldHaveNoInteractions();
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void 사진과_영수증은_기록_삭제가_커밋된_뒤_즉시_정리한다() {
        // given
        ConsumptionImage image = temporaryImage();
        ReceiptImage receipt = temporaryReceipt();
        Long consumptionId = create(userId, image.getId(), receipt.getId());
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(consumptions.existsById(consumptionId)).isFalse();
            assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.DELETING);
            assertThat(receipts.findById(receipt.getId()).orElseThrow().getConsumptionId()).isNull();
            return null;
        }).given(imageStorage).delete(image.getObjectKey());

        // when
        deletion.delete(userId, consumptionId);

        // then
        assertThat(images.existsById(image.getId())).isFalse();
        assertThat(receipts.existsById(receipt.getId())).isFalse();
        then(imageStorage).should().delete(image.getObjectKey());
        then(receiptStorage).should().delete(receipt.getObjectKey());
    }

    @Test
    void 사진_삭제_실패는_기록_삭제와_영수증_정리를_막지_않고_스케줄러가_재시도한다() {
        // given
        ConsumptionImage image = temporaryImage();
        ReceiptImage receipt = temporaryReceipt();
        Long consumptionId = create(userId, image.getId(), receipt.getId());
        willThrow(new IllegalStateException("S3 일시 오류")).willDoNothing()
                .given(imageStorage).delete(image.getObjectKey());

        // when
        deletion.delete(userId, consumptionId);

        // then
        assertThat(consumptions.existsById(consumptionId)).isFalse();
        assertThat(receipts.existsById(receipt.getId())).isFalse();
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.DELETING);
        assertThat(imageCleanup.cleanupExpiredImages()).isOne();
        assertThat(images.existsById(image.getId())).isFalse();
        then(imageStorage).should(times(2)).delete(image.getObjectKey());
    }

    @Test
    void 스티커_기록의_영수증_삭제에_실패하면_연결을_끊고_재시도한다() {
        // given
        ReceiptImage receipt = temporaryReceipt();
        Long consumptionId = create(userId, null, receipt.getId());
        willThrow(new IllegalStateException("S3 일시 오류")).willDoNothing()
                .given(receiptStorage).delete(receipt.getObjectKey());

        // when
        deletion.delete(userId, consumptionId);

        // then
        assertThat(consumptions.existsById(consumptionId)).isFalse();
        ReceiptImage pending = receipts.findById(receipt.getId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(ReceiptImageStatus.DELETING);
        assertThat(pending.getConsumptionId()).isNull();
        assertThat(receiptCleanup.cleanupExpiredImages()).isOne();
        assertThat(receipts.existsById(receipt.getId())).isFalse();
    }

    @Test
    void 다른_사용자의_기록과_없는_기록은_같은_404를_반환하고_파일을_보존한다() throws Exception {
        // given
        ConsumptionImage image = temporaryImage();
        ReceiptImage receipt = temporaryReceipt();
        Long consumptionId = create(userId, image.getId(), receipt.getId());
        Long otherUserId = users.save(User.create("다른회원")).getId();

        // when & then
        for (Long targetId : List.of(consumptionId, Long.MAX_VALUE)) {
            mvc.perform(delete("/v1/consumptions/{consumptionId}", targetId)
                            .with(jwt().jwt(token -> token.subject(otherUserId.toString()))
                                    .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ConsumptionErrorCode.CONSUMPTION_NOT_FOUND.getCode()));
        }
        assertThat(consumptions.existsById(consumptionId)).isTrue();
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
        assertThat(receipts.findById(receipt.getId()).orElseThrow().getConsumptionId()).isEqualTo(consumptionId);
        then(imageStorage).shouldHaveNoInteractions();
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void 인증하지_않으면_삭제할_수_없다() throws Exception {
        // given
        Long consumptionId = create(userId, null, null);

        // when & then
        mvc.perform(delete("/v1/consumptions/{consumptionId}", consumptionId))
                .andExpect(status().isUnauthorized());
        assertThat(consumptions.existsById(consumptionId)).isTrue();
    }

    @Test
    void DB_작업이_롤백되면_기록과_사진과_영수증_연결을_보존한다() {
        // given
        ConsumptionImage image = temporaryImage();
        ReceiptImage receipt = temporaryReceipt();
        Long consumptionId = create(userId, image.getId(), receipt.getId());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // when
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            commands.delete(userId, consumptionId);
            consumptions.flush();
            throw new IllegalStateException("삭제 롤백");
        })).isInstanceOf(IllegalStateException.class).hasMessage("삭제 롤백");

        // then
        assertThat(consumptions.existsById(consumptionId)).isTrue();
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
        ReceiptImage attached = receipts.findById(receipt.getId()).orElseThrow();
        assertThat(attached.getStatus()).isEqualTo(ReceiptImageStatus.ATTACHED);
        assertThat(attached.getConsumptionId()).isEqualTo(consumptionId);
        then(imageStorage).shouldHaveNoInteractions();
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void 동시에_삭제해도_한_번만_삭제하고_나머지는_404로_처리한다() throws Exception {
        // given
        ConsumptionImage image = temporaryImage();
        Long consumptionId = create(userId, image.getId(), null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("삭제 시작 대기 시간 초과");
                }
                try {
                    deletion.delete(userId, consumptionId);
                    return "DELETED";
                } catch (BusinessException exception) {
                    return exception.getErrorCode().getCode();
                }
            })).toList();

            // when
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS));

            // then
            assertThat(results).containsExactlyInAnyOrder("DELETED", ConsumptionErrorCode.CONSUMPTION_NOT_FOUND.getCode());
            assertThat(consumptions.existsById(consumptionId)).isFalse();
            assertThat(images.existsById(image.getId())).isFalse();
            then(imageStorage).should().delete(image.getObjectKey());
        }
    }

    private ConsumptionImage temporaryImage() {
        return images.save(ConsumptionImage.createTemporary(
                userId, UUID.randomUUID().toString(), "image/png", 100, LocalDateTime.now().plusHours(24)
        ));
    }

    private ReceiptImage temporaryReceipt() {
        return receipts.save(ReceiptImage.createTemporary(
                userId, UUID.randomUUID().toString(), "image/png", 100, LocalDateTime.now().plusHours(24)
        ));
    }

    private Long create(Long ownerId, Long imageId, Long receiptId) {
        return commands.create(new ConsumptionSaveCommand(
                ownerId, receiptId,
                new PlaceResolveCommand("test", "테스트카페", "서울특별시 강남구", 37.506481, 127.024551),
                LocalDate.now(), LocalTime.NOON, null, "카페", imageId == null ? stickerId : null, imageId, null
        ), placeId).consumptionId();
    }
}
