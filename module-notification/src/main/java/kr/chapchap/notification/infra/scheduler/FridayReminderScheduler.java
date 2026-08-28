package kr.chapchap.notification.infra.scheduler;

import kr.chapchap.notification.application.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FridayReminderScheduler {
    private final PushNotificationService pushNotificationService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
//    @Scheduled(cron = "0 0 18 * * FRI", zone = "Asia/Seoul")
    public void sendFridayReminder() {
        log.info("금요일 리마인더 발송 시작");
        pushNotificationService.notifyFridayReminderToAll();
        log.info("금요일 리마인더 발송 종료");
    }
}