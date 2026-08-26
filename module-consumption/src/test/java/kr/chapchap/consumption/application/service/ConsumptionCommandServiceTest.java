package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;
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
    void 영수증_없이_소비_기록과_획득한_스티커를_저장한다() {
        // given
        ConsumptionCreateCommand command = createCommand(null);
        StickerItem stickerItem = stickerItem(7L, "공통", "눈");
        given(stickerItemRepository.findAllByCategoryIn(List.of("카페", "공통")))
                .willReturn(List.of(stickerItem));
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
        ConsumptionCommandService service = createService();

        // when
        ConsumptionCreateInfo result = service.create(command, PLACE_ID);

        // then
        assertThat(result.consumptionId()).isEqualTo(CONSUMPTION_ID);
        assertThat(result.stickerCategory()).isEqualTo("공통");
        assertThat(result.stickerName()).isEqualTo("눈");

        ArgumentCaptor<Consumption> consumptionCaptor = ArgumentCaptor.forClass(Consumption.class);
        then(consumptionRepository).should().save(consumptionCaptor.capture());
        assertThat(consumptionCaptor.getValue().getStickerItemId()).isEqualTo(7L);
        then(receiptImageRepository).shouldHaveNoInteractions();
    }

    @Test
    void 소비_카테고리와_공통_스티커_중_하나를_선택하고_영수증을_연결한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        StickerItem stickerItem = stickerItem(7L, "공통", "따봉");
        ReceiptImage receiptImage = createTemporaryReceiptImage(NOW.plusHours(1));

        given(stickerItemRepository.findAllByCategoryIn(List.of("카페", "공통")))
                .willReturn(List.of(stickerItem));
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
        inOrder.verify(consumptionRepository).countByUserIdAndPlaceId(USER_ID, PLACE_ID);
        inOrder.verify(stickerItemRepository).findAllByCategoryIn(List.of("카페", "공통"));
        inOrder.verify(consumptionRepository).save(any(Consumption.class));
        inOrder.verify(receiptImageRepository).findByIdAndUserIdForUpdate(RECEIPT_IMAGE_ID, USER_ID);
    }

    @Test
    void 같은_장소의_여섯_번째_방문이면_왕관_스티커를_저장한다() {
        // given
        ConsumptionCreateCommand command = createCommand(null);
        StickerItem crown = stickerItem(12L, "스페셜", "왕관");
        given(consumptionRepository.countByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(5L);
        given(stickerItemRepository.findByCategoryAndName("스페셜", "왕관"))
                .willReturn(Optional.of(crown));
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
        ConsumptionCommandService service = createService();

        // when
        ConsumptionCreateInfo result = service.create(command, PLACE_ID);

        // then
        assertThat(result.stickerCategory()).isEqualTo("스페셜");
        assertThat(result.stickerName()).isEqualTo("왕관");

        ArgumentCaptor<Consumption> consumptionCaptor = ArgumentCaptor.forClass(Consumption.class);
        then(consumptionRepository).should().save(consumptionCaptor.capture());
        assertThat(consumptionCaptor.getValue().getStickerItemId()).isEqualTo(12L);
    }

    @Test
    void 같은_장소의_네_번째_방문이면_다시_일반_스티커를_저장한다() {
        // given
        ConsumptionCreateCommand command = createCommand(null);
        StickerItem common = stickerItem(10L, "공통", "눈");
        given(consumptionRepository.countByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(3L);
        given(stickerItemRepository.findAllByCategoryIn(List.of("카페", "공통")))
                .willReturn(List.of(common));
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
        ConsumptionCommandService service = createService();

        // when
        ConsumptionCreateInfo result = service.create(command, PLACE_ID);

        // then
        assertThat(result.stickerCategory()).isEqualTo("공통");
        assertThat(result.stickerName()).isEqualTo("눈");
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

    @Test
    void 정리_중인_영수증_이미지면_만료_예외가_발생한다() {
        // given
        ConsumptionCreateCommand command = createCommand(RECEIPT_IMAGE_ID);
        ReceiptImage receiptImage = createTemporaryReceiptImage(NOW.minusSeconds(1));
        receiptImage.markDeleting(NOW);
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
        StickerItem stickerItem = stickerItem(7L, "공통", "눈");
        given(stickerItemRepository.findAllByCategoryIn(List.of("카페", "공통")))
                .willReturn(List.of(stickerItem));
        given(consumptionRepository.save(any(Consumption.class))).willAnswer(invocation -> {
            Consumption consumption = invocation.getArgument(0);
            ReflectionTestUtils.setField(consumption, "id", CONSUMPTION_ID);
            return consumption;
        });
    }

    private StickerItem stickerItem(Long id, String category, String name) {
        StickerItem stickerItem = org.mockito.Mockito.mock(StickerItem.class);
        given(stickerItem.getId()).willReturn(id);
        org.mockito.Mockito.lenient().when(stickerItem.getCategory()).thenReturn(category);
        org.mockito.Mockito.lenient().when(stickerItem.getName()).thenReturn(name);
        return stickerItem;
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
