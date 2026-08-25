package kr.chapchap.account.infra.scheduler;

import kr.chapchap.account.application.service.WithdrawnUserCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class WithdrawnUserCleanupScheduler {

    private final WithdrawnUserCleanupService withdrawnUserCleanupService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void cleanupWithdrawnUsers() {
        withdrawnUserCleanupService.cleanupWithdrawnUsers();
    }
}
