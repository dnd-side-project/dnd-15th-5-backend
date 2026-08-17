package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.event.ReceiptImageCleanupEvent;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ReceiptImageCommandServiceTest {

    @Mock
    private ReceiptImageRepository receiptImageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void 이미지_정보를_저장하기_전에_롤백용_삭제_이벤트를_발행한다() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 8, 17, 9, 0);
        given(receiptImageRepository.save(any(ReceiptImage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        ReceiptImageCommandService service = new ReceiptImageCommandService(
                receiptImageRepository,
                eventPublisher
        );

        // when
        ReceiptImage result = service.saveTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                3L,
                expiresAt
        );

        // then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getObjectKey()).isEqualTo("receipts/1/receipt-key");
        assertThat(result.getContentType()).isEqualTo("image/png");
        assertThat(result.getFileSizeBytes()).isEqualTo(3L);
        assertThat(result.getStatus()).isEqualTo(ReceiptImageStatus.TEMPORARY);
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.getConsumptionId()).isNull();

        InOrder inOrder = inOrder(eventPublisher, receiptImageRepository);
        inOrder.verify(eventPublisher).publishEvent(
                new ReceiptImageCleanupEvent("receipts/1/receipt-key")
        );
        inOrder.verify(receiptImageRepository).save(result);
    }

    @Test
    void 이미지_정보_생성에_실패해도_업로드한_이미지의_삭제_이벤트를_발행한다() {
        // given
        ReceiptImageCommandService service = new ReceiptImageCommandService(
                receiptImageRepository,
                eventPublisher
        );

        // when & then
        assertThatThrownBy(() -> service.saveTemporary(
                0L,
                "receipts/0/receipt-key",
                "image/png",
                3L,
                LocalDateTime.of(2026, 8, 17, 9, 0)
        )).isInstanceOf(IllegalArgumentException.class);

        then(eventPublisher).should().publishEvent(
                new ReceiptImageCleanupEvent("receipts/0/receipt-key")
        );
        then(receiptImageRepository).shouldHaveNoInteractions();
    }
}
