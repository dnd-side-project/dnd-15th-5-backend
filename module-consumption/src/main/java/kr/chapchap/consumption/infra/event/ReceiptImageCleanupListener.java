package kr.chapchap.consumption.infra.event;

import kr.chapchap.consumption.application.event.ReceiptImageCleanupEvent;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReceiptImageCleanupListener {

    private final ReceiptImageStorage receiptImageStorage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteRolledBackImage(ReceiptImageCleanupEvent event) {
        try {
            receiptImageStorage.delete(event.objectKey());
        } catch (RuntimeException exception) {
            log.error("롤백된 영수증 이미지 정리에 실패했습니다.", exception);
        }
    }
}
