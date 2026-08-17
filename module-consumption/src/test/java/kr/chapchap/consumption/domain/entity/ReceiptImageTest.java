package kr.chapchap.consumption.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptImageTest {

    @Test
    void 임시_영수증_이미지를_소비_기록에_연결한다() {
        // given
        LocalDateTime attachedAt = LocalDateTime.of(2026, 8, 16, 12, 0);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                attachedAt.plusHours(1)
        );

        // when
        receiptImage.attach(10L, attachedAt);

        // then
        assertThat(receiptImage.getStatus()).isEqualTo(ReceiptImageStatus.ATTACHED);
        assertThat(receiptImage.getConsumptionId()).isEqualTo(10L);
        assertThat(receiptImage.getAttachedAt()).isEqualTo(attachedAt);
        assertThat(receiptImage.getExpiresAt()).isNull();
    }

    @Test
    void 만료_시각이_없는_영수증_이미지를_소비_기록에_연결한다() {
        // given
        LocalDateTime attachedAt = LocalDateTime.of(2026, 8, 16, 12, 0);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                null
        );

        // when
        receiptImage.attach(10L, attachedAt);

        // then
        assertThat(receiptImage.getStatus()).isEqualTo(ReceiptImageStatus.ATTACHED);
        assertThat(receiptImage.getConsumptionId()).isEqualTo(10L);
        assertThat(receiptImage.getAttachedAt()).isEqualTo(attachedAt);
    }

    @Test
    void 만료된_영수증_이미지는_소비_기록에_연결할_수_없다() {
        // given
        LocalDateTime attachedAt = LocalDateTime.of(2026, 8, 16, 12, 0);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                attachedAt.minusSeconds(1)
        );

        // when & then
        assertThatThrownBy(() -> receiptImage.attach(10L, attachedAt))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 만료된_임시_영수증_이미지를_정리_중_상태로_변경한다() {
        // given
        LocalDateTime cleanupAt = LocalDateTime.of(2026, 8, 17, 3, 0);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                cleanupAt.minusSeconds(1)
        );

        // when
        receiptImage.markDeleting(cleanupAt);

        // then
        assertThat(receiptImage.getStatus()).isEqualTo(ReceiptImageStatus.DELETING);
    }

    @Test
    void 만료되지_않은_영수증_이미지는_정리_중_상태로_변경할_수_없다() {
        // given
        LocalDateTime cleanupAt = LocalDateTime.of(2026, 8, 17, 3, 0);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                cleanupAt.plusSeconds(1)
        );

        // when & then
        assertThatThrownBy(() -> receiptImage.markDeleting(cleanupAt))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 이미_연결된_영수증_이미지는_다시_연결할_수_없다() {
        // given
        LocalDateTime attachedAt = LocalDateTime.of(2026, 8, 16, 12, 0);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                1L,
                "receipts/1/receipt-key",
                "image/png",
                1024L,
                attachedAt.plusHours(1)
        );
        receiptImage.attach(10L, attachedAt);

        // when & then
        assertThatThrownBy(() -> receiptImage.attach(11L, attachedAt.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
