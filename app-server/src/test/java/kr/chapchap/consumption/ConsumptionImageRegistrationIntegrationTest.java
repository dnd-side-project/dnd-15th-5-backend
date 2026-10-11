package kr.chapchap.consumption;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.command.ConsumptionImageUploadCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.service.ConsumptionCommandService;
import kr.chapchap.consumption.application.service.ConsumptionImageCommandService;
import kr.chapchap.consumption.application.service.ConsumptionImageUploadService;
import kr.chapchap.consumption.application.service.StickerQueryService;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.ConsumptionImageStatus;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConsumptionImageRegistrationIntegrationTest {
    @Autowired private ConsumptionCommandService registration;
    @Autowired private ConsumptionImageUploadService upload;
    @Autowired private ConsumptionImageCommandService imageCommands;
    @Autowired private ConsumptionImageRepository images;
    @Autowired private ReceiptImageRepository receipts;
    @Autowired private UserRepository users;
    @Autowired private StickerQueryService stickers;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private ConsumptionImageStorage storage;
    private Long userId;
    private Long placeId;

    @BeforeEach
    void setUp() {
        User user = User.create("사진등록");
        user.completeTermsAgreement();
        userId = users.save(user).getId();
        placeId = jdbc.queryForObject("""
                INSERT INTO places (google_place_id, name, road_address, administrative_dong_code,
                    administrative_dong_name, location)
                VALUES (?, '사진카페', '서울특별시 강남구', '11680650', '역삼1동',
                    ST_SetSRID(ST_MakePoint(127.024551, 37.506481), 4326)::geography) RETURNING id
                """, Long.class, UUID.randomUUID().toString());
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM receipt_images");
        jdbc.update("DELETE FROM consumptions");
        jdbc.update("DELETE FROM consumption_images");
        jdbc.update("DELETE FROM places");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void PNG를_임시_저장하고_금액_없이_사진과_영수증과_천자_메모를_함께_등록한다() throws Exception {
        // given
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        byte[] content = bytes.toByteArray();
        given(storage.store(userId, content, "image/png")).willReturn("consumption-images/test/photo");
        var uploaded = upload.upload(new ConsumptionImageUploadCommand(userId, content));
        assertThat(uploaded.expiresAt()).isBetween(LocalDateTime.now().plusHours(23), LocalDateTime.now().plusHours(25));
        ReceiptImage receipt = receipts.save(ReceiptImage.createTemporary(userId, "receipt/test", "image/png", 100,
                LocalDateTime.now().plusHours(24)));
        // when
        var result = registration.create(command(uploaded.imageId(), receipt.getId()), placeId);
        // then
        var consumption = jdbc.queryForMap("SELECT * FROM consumptions WHERE id = ?", result.consumptionId());
        assertThat(consumption.get("image_id")).isEqualTo(uploaded.imageId());
        assertThat(consumption.get("amount")).isNull();
        assertThat(consumption.get("sticker_item_id")).isNull();
        assertThat(consumption.get("memo")).isEqualTo("가".repeat(1000));
        assertThat(result.stickerName()).isNull();
        assertThat(images.findById(uploaded.imageId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
        assertThat(images.findById(uploaded.imageId()).orElseThrow().getExpiresAt()).isNull();
        assertThat(receipts.findById(receipt.getId()).orElseThrow().getConsumptionId()).isEqualTo(result.consumptionId());
        assertThat(imageCommands.prepareForCleanup(uploaded.imageId(), LocalDateTime.now().plusDays(2))).isEmpty();
    }

    @Test
    void 영수증_연결에_실패하면_사진_연결과_소비기록도_롤백한다() {
        // given
        ConsumptionImage image = temporary(userId, LocalDateTime.now().plusHours(24));
        // when & then
        assertError(() -> registration.create(command(image.getId(), Long.MAX_VALUE), placeId),
                ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND);
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.TEMPORARY);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consumptions", Long.class)).isZero();
    }

    @Test
    void 다른_사용자의_사진을_연결할_수_없다() {
        // given
        User other = users.save(User.create("사진주인"));
        ConsumptionImage image = temporary(other.getId(), LocalDateTime.now().plusHours(24));
        // when & then
        assertError(() -> registration.create(command(image.getId(), null), placeId), ConsumptionErrorCode.IMAGE_NOT_FOUND);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consumptions", Long.class)).isZero();
    }

    @Test
    void 만료되거나_삭제_중인_사진은_등록할_수_없다() {
        // given
        ConsumptionImage image = temporary(userId, LocalDateTime.now().minusSeconds(1));
        // when & then
        assertError(() -> registration.create(command(image.getId(), null), placeId), ConsumptionErrorCode.IMAGE_EXPIRED);
        imageCommands.prepareForCleanup(image.getId(), LocalDateTime.now());
        assertError(() -> registration.create(command(image.getId(), null), placeId), ConsumptionErrorCode.IMAGE_EXPIRED);
    }

    @Test
    void 동일한_사진으로_동시_등록해도_한_기록에만_연결된다() throws Exception {
        // given
        ConsumptionImage image = temporary(userId, LocalDateTime.now().plusHours(24));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("등록 시작 대기 시간 초과");
                }
                try {
                    registration.create(command(image.getId(), null), placeId);
                    return "CREATED";
                } catch (BusinessException exception) {
                    return exception.getErrorCode().getCode();
                }
            })).toList();
            // when
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS));
            // then
            assertThat(results).containsExactlyInAnyOrder("CREATED", ConsumptionErrorCode.IMAGE_ALREADY_ATTACHED.getCode());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consumptions WHERE image_id = ?", Long.class, image.getId())).isOne();
        }
    }

    @Test
    void 등록이_사진을_잠근_동안_정리는_대기하고_연결된_사진을_보존한다() throws Exception {
        // given
        ConsumptionImage image = temporary(userId, LocalDateTime.now().plusHours(24));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch cleanupStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attaching = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                images.findByIdForUpdate(image.getId()).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("정리 시작 대기 시간 초과");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return registration.create(command(image.getId(), null), placeId);
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var cleaning = executor.submit(() -> {
                cleanupStarted.countDown();
                return imageCommands.prepareForCleanup(image.getId(), LocalDateTime.now().plusDays(2));
            });
            // when
            assertThat(cleanupStarted.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            attaching.get(10, TimeUnit.SECONDS);
            // then
            assertThat(cleaning.get(10, TimeUnit.SECONDS)).isEmpty();
            assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
        }
    }

    @Test
    void 등록용_스티커_목록은_카테고리별_여덟개씩_반환한다() {
        // when
        var catalog = stickers.findAll();
        // then
        assertThat(catalog).hasSize(64);
        assertThat(catalog.stream().collect(java.util.stream.Collectors.groupingBy(
                kr.chapchap.consumption.application.info.StickerItemInfo::category,
                java.util.stream.Collectors.counting())).values()).containsOnly(8L).hasSize(8);
    }

    private ConsumptionImage temporary(Long owner, LocalDateTime expiresAt) {
        return images.save(ConsumptionImage.createTemporary(owner, UUID.randomUUID().toString(), "image/png", 100, expiresAt));
    }

    private ConsumptionCreateCommand command(Long imageId, Long receiptId) {
        return new ConsumptionCreateCommand(userId, receiptId,
                new PlaceResolveCommand("test", "사진카페", "서울특별시 강남구", 37.506481, 127.024551),
                LocalDate.now(), LocalTime.NOON, null, "카페", null, imageId, "가".repeat(1000));
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ConsumptionErrorCode expected) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
