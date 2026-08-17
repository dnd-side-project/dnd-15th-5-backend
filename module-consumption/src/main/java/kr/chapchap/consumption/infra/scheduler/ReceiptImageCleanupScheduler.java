package kr.chapchap.consumption.infra.scheduler;

import kr.chapchap.consumption.application.service.ReceiptImageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ReceiptImageCleanupScheduler {

    private final ReceiptImageCleanupService receiptImageCleanupService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredReceiptImages() {
        receiptImageCleanupService.cleanupExpiredImages();
    }
}
