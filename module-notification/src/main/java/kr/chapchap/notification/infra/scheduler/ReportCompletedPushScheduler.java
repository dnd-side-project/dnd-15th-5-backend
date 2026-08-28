package kr.chapchap.notification.infra.scheduler;

import kr.chapchap.notification.application.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportCompletedPushScheduler {

    private final PushNotificationService pushNotificationService;
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
//    @Scheduled(cron = "0 0 8 1 * *", zone = "Asia/Seoul")
    public void sendReportCompletedPush() {
        log.info("리포트 완성 알림 발송 시작");
        pushNotificationService.sendPendingReportCompletedPush();
        log.info("리포트 완성 알림 발송 종료");
    }
}
