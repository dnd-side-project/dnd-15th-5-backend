package kr.chapchap.consumption;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.command.ConsumptionSaveCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.port.PlaceResolvePort;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.application.service.ConsumptionCommandService;
import kr.chapchap.consumption.application.service.ConsumptionImageCleanupService;
import kr.chapchap.consumption.application.service.ConsumptionUpdateService;
import kr.chapchap.consumption.application.service.ReceiptImageCleanupService;
import kr.chapchap.consumption.domain.entity.Consumption;
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
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.service.PlacePhotoService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsumptionUpdateIntegrationTest {

    @Autowired private ConsumptionCommandService commands;
    @Autowired private ConsumptionUpdateService update;
    @Autowired private ConsumptionRepository consumptions;
    @Autowired private ConsumptionImageRepository images;
    @Autowired private ReceiptImageRepository receipts;
    @Autowired private ConsumptionImageCleanupService imageCleanup;
    @Autowired private ReceiptImageCleanupService receiptCleanup;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private ConsumptionImageStorage imageStorage;
    @MockitoBean private ReceiptImageStorage receiptStorage;
    @MockitoBean private PlaceResolvePort places;
    @MockitoBean private PlacePhotoService photos;

    private Long userId;
    private Long placeId;
    private Long stickerId;
    private final LocalDate date = LocalDate.of(2026, 10, 1);

    @BeforeEach
    void setUp() {
        User user = User.create("수정테스트");
        user.completeTermsAgreement();
        userId = users.save(user).getId();
        placeId = createPlace("existing-place");
        stickerId = jdbc.queryForObject("SELECT id FROM sticker_item WHERE category = '카페' AND name = '커피'", Long.class);
        given(places.resolve(any())).willReturn(placeId);
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
    void 스티커_기록을_조회하면_수정에_필요한_기존_입력값을_반환한다() throws Exception {
        // given
        Long id = create(null, null);
        given(photos.findThumbnails(any())).willReturn(Map.of(placeId, new PlacePhotoInfo("https://example.com/place", null)));

        // when & then
        mvc.perform(get("/v1/consumptions/{id}", id).with(userJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consumptionId").value(id))
                .andExpect(jsonPath("$.data.placeId").value(placeId))
                .andExpect(jsonPath("$.data.googlePlaceId").value("existing-place"))
                .andExpect(jsonPath("$.data.placeName").value("테스트카페"))
                .andExpect(jsonPath("$.data.roadAddress").value("서울특별시 강남구"))
                .andExpect(jsonPath("$.data.latitude").value(37.506481))
                .andExpect(jsonPath("$.data.longitude").value(127.024551))
                .andExpect(jsonPath("$.data.purchaseDate").value("2026-10-01"))
                .andExpect(jsonPath("$.data.purchaseTime").value("12:00:00"))
                .andExpect(jsonPath("$.data.amount").value(5500))
                .andExpect(jsonPath("$.data.category").value("카페"))
                .andExpect(jsonPath("$.data.memo").value("기존 메모"))
                .andExpect(jsonPath("$.data.stickerItemId").value(stickerId))
                .andExpect(jsonPath("$.data.stickerName").value("커피"))
                .andExpect(jsonPath("$.data.stickerCategory").value("카페"))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("https://example.com/place"))
                .andExpect(jsonPath("$.data.imageId").isEmpty())
                .andExpect(jsonPath("$.data.imageUrl").isEmpty());
        then(imageStorage).shouldHaveNoInteractions();
    }

    @Test
    void 사진_기록을_조회하면_본인의_사진_URL을_반환하고_영수증_URL은_반환하지_않는다() throws Exception {
        // given
        ConsumptionImage image = image(userId);
        Long id = create(image.getId(), receipt().getId());
        given(imageStorage.createReadUrl(image.getObjectKey())).willReturn("https://example.com/signed-photo");

        // when & then
        mvc.perform(get("/v1/consumptions/{id}", id).with(userJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageId").value(image.getId()))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/signed-photo"))
                .andExpect(jsonPath("$.data.stickerItemId").isEmpty())
                .andExpect(jsonPath("$.data.stickerName").isEmpty())
                .andExpect(jsonPath("$.data.thumbnailUrl").isEmpty())
                .andExpect(jsonPath("$.data.receiptImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.objectKey").doesNotExist());
    }

    @Test
    void 기록_ID를_유지하며_장소와_스티커와_일시를_바꾸고_금액과_메모를_비운다() throws Exception {
        // given
        Long id = create(null, null);
        Long newPlaceId = createPlace("new-place");
        Long newStickerId = jdbc.queryForObject("SELECT MIN(id) FROM sticker_item WHERE category = '운동'", Long.class);
        given(places.resolve(any())).willReturn(newPlaceId);
        Map<String, Object> body = body(null, null);
        body.put("googlePlaceId", "new-place");
        body.put("stickerItemId", newStickerId);
        body.put("category", "운동");
        body.put("purchaseDate", "2026-09-30");
        body.put("purchaseTime", "18:30:00");
        body.put("amount", null);
        body.put("memo", "   ");

        // when & then
        mvc.perform(put("/v1/consumptions/{id}", id).with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk());
        Consumption changed = consumptions.findById(id).orElseThrow();
        assertThat(consumptions.count()).isOne();
        assertThat(changed.getPlaceId()).isEqualTo(newPlaceId);
        assertThat(changed.getStickerItemId()).isEqualTo(newStickerId);
        assertThat(changed.getCategory()).isEqualTo("운동");
        assertThat(changed.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(changed.getPurchaseTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(changed.getAmount()).isNull();
        assertThat(changed.getMemo()).isNull();
    }

    @Test
    void 기존_사진과_영수증_ID를_다시_보내거나_영수증을_생략하면_연결을_유지한다() throws Exception {
        // given
        ConsumptionImage image = image(userId);
        ReceiptImage receipt = receipt();
        Long id = create(image.getId(), receipt.getId());
        LocalDateTime attachedAt = images.findById(image.getId()).orElseThrow().getAttachedAt();

        // when
        update.update(id, command(image.getId(), receipt.getId()));
        Map<String, Object> body = body(image.getId(), null);
        body.remove("receiptImageId");
        body.remove("amount");
        body.remove("memo");
        mvc.perform(put("/v1/consumptions/{id}", id).with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk());

        // then
        Consumption changed = consumptions.findById(id).orElseThrow();
        assertThat(changed.getImageId()).isEqualTo(image.getId());
        assertThat(changed.getAmount()).isNull();
        assertThat(changed.getMemo()).isNull();
        assertThat(images.findById(image.getId()).orElseThrow().getAttachedAt()).isEqualTo(attachedAt);
        assertThat(receipts.findById(receipt.getId()).orElseThrow().getConsumptionId()).isEqualTo(id);
        then(imageStorage).shouldHaveNoInteractions();
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void 사진과_영수증을_교체하면_커밋_후_기존_파일만_즉시_삭제한다() {
        // given
        ConsumptionImage oldImage = image(userId);
        ConsumptionImage newImage = image(userId);
        ReceiptImage oldReceipt = receipt();
        ReceiptImage newReceipt = receipt();
        Long id = create(oldImage.getId(), oldReceipt.getId());
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(consumptions.findById(id).orElseThrow().getImageId()).isEqualTo(newImage.getId());
            assertThat(images.findById(newImage.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
            assertThat(receipts.findById(newReceipt.getId()).orElseThrow().getConsumptionId()).isEqualTo(id);
            return null;
        }).given(imageStorage).delete(oldImage.getObjectKey());

        // when
        update.update(id, command(newImage.getId(), newReceipt.getId()));

        // then
        assertThat(images.existsById(oldImage.getId())).isFalse();
        assertThat(receipts.existsById(oldReceipt.getId())).isFalse();
        then(imageStorage).should().delete(oldImage.getObjectKey());
        then(imageStorage).shouldHaveNoMoreInteractions();
        then(receiptStorage).should().delete(oldReceipt.getObjectKey());
        then(receiptStorage).shouldHaveNoMoreInteractions();
    }

    @Test
    void 기존_파일_삭제가_실패해도_수정은_유지하고_스케줄러에서_재시도한다() {
        // given
        ConsumptionImage oldImage = image(userId);
        ReceiptImage oldReceipt = receipt();
        Long id = create(oldImage.getId(), oldReceipt.getId());
        ConsumptionImage newImage = image(userId);
        ReceiptImage newReceipt = receipt();
        willThrow(new IllegalStateException("S3 실패")).willDoNothing().given(imageStorage).delete(oldImage.getObjectKey());
        willThrow(new IllegalStateException("S3 실패")).willDoNothing().given(receiptStorage).delete(oldReceipt.getObjectKey());

        // when
        update.update(id, command(newImage.getId(), newReceipt.getId()));

        // then
        assertThat(consumptions.findById(id).orElseThrow().getImageId()).isEqualTo(newImage.getId());
        assertThat(images.findById(oldImage.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.DELETING);
        assertThat(receipts.findById(oldReceipt.getId()).orElseThrow().getStatus()).isEqualTo(ReceiptImageStatus.DELETING);
        assertThat(imageCleanup.cleanupExpiredImages()).isOne();
        assertThat(receiptCleanup.cleanupExpiredImages()).isOne();
        assertThat(images.existsById(newImage.getId())).isTrue();
        assertThat(receipts.existsById(newReceipt.getId())).isTrue();
    }

    @Test
    void 영수증_검증이_실패하면_사진_교체와_입력값_수정도_롤백한다() {
        // given
        ConsumptionImage oldImage = image(userId);
        ReceiptImage oldReceipt = receipt();
        ConsumptionImage newImage = image(userId);
        Long id = create(oldImage.getId(), oldReceipt.getId());

        // when & then
        assertError(() -> update.update(id, command(newImage.getId(), Long.MAX_VALUE)), ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND);
        assertOriginal(id, oldImage, oldReceipt);
        assertThat(images.findById(newImage.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.TEMPORARY);
        then(imageStorage).shouldHaveNoInteractions();
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void DB_커밋이_실패하면_새_파일은_임시로_돌아가고_기존_파일을_삭제하지_않는다() {
        // given
        ConsumptionImage oldImage = image(userId);
        ReceiptImage oldReceipt = receipt();
        Long id = create(oldImage.getId(), oldReceipt.getId());
        ConsumptionImage newImage = image(userId);
        ReceiptImage newReceipt = receipt();
        given(places.resolve(any())).willReturn(Long.MAX_VALUE);

        // when & then
        assertThatThrownBy(() -> update.update(id, command(newImage.getId(), newReceipt.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertOriginal(id, oldImage, oldReceipt);
        assertThat(images.findById(newImage.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.TEMPORARY);
        assertThat(receipts.findById(newReceipt.getId()).orElseThrow().getStatus()).isEqualTo(ReceiptImageStatus.TEMPORARY);
        then(imageStorage).shouldHaveNoInteractions();
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void 타인_사진이나_만료되거나_이미_사용된_사진으로_교체할_수_없다() {
        // given
        ConsumptionImage oldImage = image(userId);
        Long id = create(oldImage.getId(), null);
        Long otherUserId = users.save(User.create("다른회원")).getId();
        ConsumptionImage otherImage = image(otherUserId);
        ConsumptionImage expired = images.save(ConsumptionImage.createTemporary(
                userId, UUID.randomUUID().toString(), "image/png", 100, LocalDateTime.now().minusSeconds(1)));
        ConsumptionImage used = image(userId);
        create(used.getId(), null);

        // when & then
        assertError(() -> update.update(id, command(otherImage.getId(), null)), ConsumptionErrorCode.IMAGE_NOT_FOUND);
        assertError(() -> update.update(id, command(expired.getId(), null)), ConsumptionErrorCode.IMAGE_EXPIRED);
        assertError(() -> update.update(id, command(used.getId(), null)), ConsumptionErrorCode.IMAGE_ALREADY_ATTACHED);
        assertThat(consumptions.findById(id).orElseThrow().getImageId()).isEqualTo(oldImage.getId());
        then(imageStorage).shouldHaveNoInteractions();
    }

    @Test
    void 사진을_스티커로_전환하면_기존_사진만_정리하고_영수증과_입력값을_유지한다() throws Exception {
        // given
        ConsumptionImage image = image(userId);
        ReceiptImage receipt = receipt();
        Long id = create(image.getId(), receipt.getId());
        Map<String, Object> body = body(null, null);
        body.put("amount", 5500);
        body.put("memo", "기존 메모");
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(consumptions.findById(id).orElseThrow().getImageId()).isNull();
            return null;
        }).given(imageStorage).delete(image.getObjectKey());

        // when
        mvc.perform(put("/v1/consumptions/{id}", id).with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk());

        // then
        Consumption changed = consumptions.findById(id).orElseThrow();
        assertThat(changed.getStickerItemId()).isEqualTo(stickerId);
        assertThat(changed.getImageId()).isNull();
        assertThat(changed.getAmount()).isEqualTo(5500L);
        assertThat(changed.getMemo()).isEqualTo("기존 메모");
        assertThat(images.existsById(image.getId())).isFalse();
        assertThat(receipts.findById(receipt.getId()).orElseThrow().getConsumptionId()).isEqualTo(id);
        then(imageStorage).should().delete(image.getObjectKey());
        then(receiptStorage).shouldHaveNoInteractions();
    }

    @Test
    void 스티커를_사진으로_전환하면_새_사진을_연결하고_지도_조회도_성공한다() throws Exception {
        // given
        Long id = create(null, null);
        ConsumptionImage image = image(userId);

        // when
        mvc.perform(put("/v1/consumptions/{id}", id).with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body(image.getId(), null))))
                .andExpect(status().isOk());

        // then
        Consumption changed = consumptions.findById(id).orElseThrow();
        assertThat(changed.getStickerItemId()).isNull();
        assertThat(changed.getImageId()).isEqualTo(image.getId());
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
        then(imageStorage).shouldHaveNoInteractions();
        mvc.perform(get("/v1/consumptions/visited-places").with(userJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places[0].placeId").value(placeId))
                .andExpect(jsonPath("$.data.places[0].stickerCategory").isEmpty())
                .andExpect(jsonPath("$.data.places[0].stickerName").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 전환_중_영수증_검증이_실패하면_기존_유형과_사진_상태를_유지한다(boolean fromPhoto) {
        // given
        ConsumptionImage image = image(userId);
        Long id = create(fromPhoto ? image.getId() : null, null);

        // when
        assertError(() -> update.update(id, command(fromPhoto ? null : image.getId(), Long.MAX_VALUE)),
                ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND);

        // then
        Consumption unchanged = consumptions.findById(id).orElseThrow();
        assertThat(unchanged.getImageId()).isEqualTo(fromPhoto ? image.getId() : null);
        assertThat(unchanged.getStickerItemId()).isEqualTo(fromPhoto ? null : stickerId);
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(
                fromPhoto ? ConsumptionImageStatus.ATTACHED : ConsumptionImageStatus.TEMPORARY);
        then(imageStorage).shouldHaveNoInteractions();
    }

    @Test
    void 수정에서도_카테고리_일치와_금액과_메모와_이미지_선택을_검증한다() throws Exception {
        // given
        Long id = create(null, null);
        List<Map<String, Object>> invalidBodies = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> entry : Map.<String, Object>of(
                "amount", 0, "memo", "가".repeat(1001), "category", "없는분류", "imageId", 9L
        ).entrySet()) {
            Map<String, Object> body = body(null, null);
            body.put(entry.getKey(), entry.getValue());
            invalidBodies.add(body);
        }
        Map<String, Object> emptySelection = body(null, null);
        emptySelection.put("stickerItemId", null);
        invalidBodies.add(emptySelection);
        Map<String, Object> wrongCategory = body(null, null);
        wrongCategory.put("category", "운동");
        invalidBodies.add(wrongCategory);

        // when & then
        for (Map<String, Object> body : invalidBodies) {
            mvc.perform(put("/v1/consumptions/{id}", id).with(userJwt(userId))
                            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(consumptions.findById(id).orElseThrow().getCategory()).isEqualTo("카페");
        assertThat(consumptions.findById(id).orElseThrow().getAmount()).isEqualTo(5500L);
    }

    @Test
    void 타인과_없는_기록은_조회와_수정_모두_404이며_외부_요청도_하지_않는다() throws Exception {
        // given
        Long id = create(null, null);
        Long otherUserId = users.save(User.create("다른회원")).getId();

        // when & then
        for (Long target : List.of(id, Long.MAX_VALUE)) {
            mvc.perform(get("/v1/consumptions/{id}", target).with(userJwt(otherUserId)))
                    .andExpect(status().isNotFound());
            mvc.perform(put("/v1/consumptions/{id}", target).with(userJwt(otherUserId))
                            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body(null, null))))
                    .andExpect(status().isNotFound());
        }
        then(places).shouldHaveNoInteractions();
        then(photos).shouldHaveNoInteractions();
        then(imageStorage).shouldHaveNoInteractions();
    }

    @Test
    void 인증하지_않으면_조회와_수정을_거절한다() throws Exception {
        // given
        Long id = create(null, null);

        // when & then
        mvc.perform(get("/v1/consumptions/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(put("/v1/consumptions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body(null, null))))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 같은_사진이나_영수증을_두_기록에_동시_연결해도_하나만_성공한다(boolean photo) throws Exception {
        // given
        Long firstId = create(photo ? image(userId).getId() : null, null);
        Long secondId = create(photo ? image(userId).getId() : null, null);
        ConsumptionSaveCommand command = command(photo ? image(userId).getId() : null, photo ? null : receipt().getId());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(firstId, secondId).stream().map(id -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("수정 시작 대기 시간 초과");
                }
                try {
                    update.update(id, command);
                    return "UPDATED";
                } catch (BusinessException exception) {
                    return exception.getErrorCode().getCode();
                }
            })).toList();

            // when
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var result = List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS));

            // then
            assertThat(result).containsExactlyInAnyOrder("UPDATED", photo
                    ? ConsumptionErrorCode.IMAGE_ALREADY_ATTACHED.getCode()
                    : ConsumptionErrorCode.RECEIPT_IMAGE_ALREADY_ATTACHED.getCode());
        }
    }

    private void assertOriginal(Long id, ConsumptionImage image, ReceiptImage receipt) {
        Consumption record = consumptions.findById(id).orElseThrow();
        assertThat(record.getImageId()).isEqualTo(image.getId());
        assertThat(record.getPlaceId()).isEqualTo(placeId);
        assertThat(record.getAmount()).isEqualTo(5500L);
        assertThat(record.getMemo()).isEqualTo("기존 메모");
        assertThat(images.findById(image.getId()).orElseThrow().getStatus()).isEqualTo(ConsumptionImageStatus.ATTACHED);
        assertThat(receipts.findById(receipt.getId()).orElseThrow().getConsumptionId()).isEqualTo(id);
    }

    private Long create(Long imageId, Long receiptId) {
        return commands.create(new ConsumptionSaveCommand(
                userId, receiptId, place(), date, LocalTime.NOON, 5500L, "카페",
                imageId == null ? stickerId : null, imageId, "기존 메모"
        ), placeId).consumptionId();
    }

    private ConsumptionSaveCommand command(Long imageId, Long receiptId) {
        return new ConsumptionSaveCommand(userId, receiptId, place(), date, LocalTime.NOON,
                null, "카페", imageId == null ? stickerId : null, imageId, "변경된 메모");
    }

    private PlaceResolveCommand place() {
        return new PlaceResolveCommand("existing-place", "테스트카페", "서울특별시 강남구", 37.506481, 127.024551);
    }

    private Long createPlace(String googlePlaceId) {
        return jdbc.queryForObject("""
                INSERT INTO places (google_place_id, name, road_address, administrative_dong_code,
                    administrative_dong_name, location)
                VALUES (?, '테스트카페', '서울특별시 강남구', '11680650', '역삼1동',
                    ST_SetSRID(ST_MakePoint(127.024551, 37.506481), 4326)::geography) RETURNING id
                """, Long.class, googlePlaceId);
    }

    private ConsumptionImage image(Long ownerId) {
        return images.save(ConsumptionImage.createTemporary(ownerId, UUID.randomUUID().toString(),
                "image/png", 100, LocalDateTime.now().plusHours(24)));
    }

    private ReceiptImage receipt() {
        return receipts.save(ReceiptImage.createTemporary(userId, UUID.randomUUID().toString(),
                "image/png", 100, LocalDateTime.now().plusHours(24)));
    }

    private Map<String, Object> body(Long imageId, Long receiptId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("googlePlaceId", "existing-place");
        body.put("placeName", "테스트카페");
        body.put("roadAddress", "서울특별시 강남구");
        body.put("latitude", 37.506481);
        body.put("longitude", 127.024551);
        body.put("purchaseDate", date.toString());
        body.put("purchaseTime", "12:00:00");
        body.put("amount", 6500);
        body.put("memo", "수정 메모");
        body.put("category", "카페");
        body.put("stickerItemId", imageId == null ? stickerId : null);
        body.put("imageId", imageId);
        body.put("receiptImageId", receiptId);
        return body;
    }

    private RequestPostProcessor userJwt(Long id) {
        return jwt().jwt(token -> token.subject(id.toString())).authorities(new SimpleGrantedAuthority("SCOPE_user"));
    }

    private void assertError(ThrowingCallable action, ConsumptionErrorCode error) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(error));
    }
}
