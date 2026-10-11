package kr.chapchap.consumption.infra.scheduler;

import kr.chapchap.consumption.application.service.ConsumptionImageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ConsumptionImageCleanupScheduler {

    private final ConsumptionImageCleanupService consumptionImageCleanupService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredConsumptionImages() {
        consumptionImageCleanupService.cleanupExpiredImages();
    }
}
