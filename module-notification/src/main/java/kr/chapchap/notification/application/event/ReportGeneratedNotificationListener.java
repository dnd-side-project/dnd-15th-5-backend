package kr.chapchap.notification.application.event;

import kr.chapchap.notification.application.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportGeneratedNotificationListener {
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportGenerated(ReportGeneratedEvent event) {
        try {
            pushNotificationService.recordReportCompleted(event.userId());
        } catch (RuntimeException exception) {
            log.error("리포트 완성 알림 발송 실패. userId={}", event.userId(), exception);
        }
    }
}