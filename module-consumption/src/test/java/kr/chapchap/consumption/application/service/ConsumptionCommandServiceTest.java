package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.info.ConsumptionInfo;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import kr.chapchap.consumption.domain.repository.StickerItemRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ConsumptionCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PLACE_ID = 101L;
    private static final Long CONSUMPTION_ID = 10L;
    private static final Long RECEIPT_IMAGE_ID = 15L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 16, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-16T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final PlaceResolveCommand PLACE = new PlaceResolveCommand(
            "google-place-id",
            "찹찹카페",
            "서울특별시 강남구 테헤란로 123",
            37.501,
            127.039
    );

    @Mock
    private ConsumptionRepository consumptionRepository;

    @Mock
    private StickerItemRepository stickerItemRepository;

    @Mock
    private ReceiptImageRepository receiptImageRepository;

    @Test
    void 영수증_없이_소비_기록을_저장한다() {
        // given
        ConsumptionCreateCommand command = createCommand(null);
        given(stickerItemRepository.findAllByCategory("카페")).willReturn(List.of());
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
        ConsumptionCommandService service = createService();

        // when
        ConsumptionInfo result = service.create(command, PLACE_ID);

        // then
        assertThat(result.id()).isEqualTo(CONSUMPTION_ID);
        assertThat(result.placeId()).isEqualTo(PLACE_ID);
        assertThat(result.placeName()).isEqualTo("찹찹카페");

        ArgumentCaptor<Consumption> consumptionCaptor = ArgumentCaptor.forClass(Consumption.class);
        then(consumptionRepository).should().save(consumptionCaptor.capture());
        assertThat(consumptionCaptor.getValue().getStickerItemId()).isNull();
        then(receiptImageRepository).shouldHaveNoInteractions();
    }

    @Test
    void 카테고리_스티커를_선택하고_영수증_이미지를_소비_기록에_연결한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        StickerItem stickerItem = org.mockito.Mockito.mock(StickerItem.class);
        given(stickerItem.getId()).willReturn(7L);
        ReceiptImage receiptImage = createTemporaryReceiptImage(NOW.plusHours(1));

        given(stickerItemRepository.findAllByCategory("카페")).willReturn(List.of(stickerItem));
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
        given(receiptImageRepository.findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID))
                .willReturn(Optional.of(receiptImage));
        ConsumptionCommandService service = createService();

        // when
        service.create(command, PLACE_ID);

        // then
        ArgumentCaptor<Consumption> consumptionCaptor = ArgumentCaptor.forClass(Consumption.class);
        then(consumptionRepository).should().save(consumptionCaptor.capture());
        assertThat(consumptionCaptor.getValue().getStickerItemId()).isEqualTo(7L);
        assertThat(receiptImage.getStatus()).isEqualTo(ReceiptImageStatus.ATTACHED);
        assertThat(receiptImage.getConsumptionId()).isEqualTo(CONSUMPTION_ID);
        assertThat(receiptImage.getAttachedAt()).isEqualTo(NOW);

        InOrder inOrder = inOrder(
                stickerItemRepository,
                consumptionRepository,
                receiptImageRepository
        );
        inOrder.verify(stickerItemRepository).findAllByCategory("카페");
        inOrder.verify(consumptionRepository).save(any(Consumption.class));
        inOrder.verify(receiptImageRepository).findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID);
    }

    @Test
    void 만료_시각이_없는_영수증_이미지를_소비_기록에_연결한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        ReceiptImage receiptImage = createTemporaryReceiptImage(null);
        givenSuccessfulConsumptionSave();
        given(receiptImageRepository.findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID))
                .willReturn(Optional.of(receiptImage));
        ConsumptionCommandService service = createService();

        // when
        service.create(command, PLACE_ID);

        // then
        assertThat(receiptImage.getStatus()).isEqualTo(ReceiptImageStatus.ATTACHED);
        assertThat(receiptImage.getConsumptionId()).isEqualTo(CONSUMPTION_ID);
        assertThat(receiptImage.getAttachedAt()).isEqualTo(NOW);
    }

    @Test
    void 사용자에게_속한_영수증_이미지가_없으면_예외가_발생한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        givenSuccessfulConsumptionSave();
        given(receiptImageRepository.findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID))
                .willReturn(Optional.empty());
        ConsumptionCommandService service = createService();

        // when & then
        assertThatThrownBy(() -> service.create(command, PLACE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND)
                );
    }

    @Test
    void 이미_연결된_영수증_이미지면_예외가_발생한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        ReceiptImage receiptImage = createTemporaryReceiptImage(NOW.plusHours(1));
        receiptImage.attach(99L, NOW.minusMinutes(1));
        givenSuccessfulConsumptionSave();
        given(receiptImageRepository.findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID))
                .willReturn(Optional.of(receiptImage));
        ConsumptionCommandService service = createService();

        // when & then
        assertThatThrownBy(() -> service.create(command, PLACE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ConsumptionErrorCode.RECEIPT_IMAGE_ALREADY_ATTACHED
                        )
                );
    }

    @Test
    void 만료된_영수증_이미지면_예외가_발생한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        ReceiptImage receiptImage = createTemporaryReceiptImage(NOW.minusSeconds(1));
        givenSuccessfulConsumptionSave();
        given(receiptImageRepository.findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID))
                .willReturn(Optional.of(receiptImage));
        ConsumptionCommandService service = createService();

        // when & then
        assertThatThrownBy(() -> service.create(command, PLACE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ConsumptionErrorCode.RECEIPT_IMAGE_EXPIRED)
                );
    }

    private ConsumptionCommandService createService() {
        return new ConsumptionCommandService(
                consumptionRepository,
                stickerItemRepository,
                receiptImageRepository,
                FIXED_CLOCK
        );
    }

    private void givenSuccessfulConsumptionSave() {
        given(stickerItemRepository.findAllByCategory("카페")).willReturn(List.of());
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
    }

    private ConsumptionCreateCommand createCommand(Long receiptImageId) {
        return new ConsumptionCreateCommand(
                USER_ID,
                receiptImageId,
                PLACE,
                LocalDate.of(2026, 8, 16),
                LocalTime.of(11, 30),
                12_000L,
                "카페"
        );
    }

    private ReceiptImage createTemporaryReceiptImage(LocalDateTime expiresAt) {
        return ReceiptImage.createTemporary(
                USER_ID,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                expiresAt
        );
    }
}
