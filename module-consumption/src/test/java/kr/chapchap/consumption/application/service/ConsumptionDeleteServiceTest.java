package kr.chapchap.consumption.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

class ConsumptionDeleteServiceTest {

    @Test
    void DB_삭제가_실패하면_파일_정리를_시도하지_않는다() {
        // given
        ConsumptionCommandService commands = mock(ConsumptionCommandService.class);
        ConsumptionImageCleanupService images = mock(ConsumptionImageCleanupService.class);
        ReceiptImageCleanupService receipts = mock(ReceiptImageCleanupService.class);
        ConsumptionDeleteService service = new ConsumptionDeleteService(commands, images, receipts);
        IllegalStateException failure = new IllegalStateException("DB 커밋 실패");
        given(commands.delete(1L, 10L)).willThrow(failure);

        // when & then
        assertThatThrownBy(() -> service.delete(1L, 10L)).isSameAs(failure);
        then(images).shouldHaveNoInteractions();
        then(receipts).shouldHaveNoInteractions();
    }
}
