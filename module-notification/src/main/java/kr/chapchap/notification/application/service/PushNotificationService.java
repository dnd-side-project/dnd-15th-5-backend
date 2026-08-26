package kr.chapchap.notification.application.service;

import kr.chapchap.notification.application.info.PushMessage;
import kr.chapchap.notification.application.info.PushSendResult;
import kr.chapchap.notification.application.info.UserPushTarget;
import kr.chapchap.notification.application.port.PushSenderPort;
import kr.chapchap.notification.application.port.UserPushTargetPort;
import kr.chapchap.notification.domain.entity.Notification;
import kr.chapchap.notification.domain.entity.NotificationType;
import kr.chapchap.notification.domain.entity.PushStatus;
import kr.chapchap.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PushNotificationService {

    private static final String REPORT_COMPLETED_TITLE = "이번 달 소비 취향이 완성됐어요!";
    private static final String REPORT_COMPLETED_BODY = "이번 달 가장 자주 찾은 동네와 새롭게 생긴 단골을 확인해 보세요.";

    private static final String FRIDAY_REMINDER_TITLE = "지갑 속에 잠자는 영수증이 있나요?";
    private static final String FRIDAY_REMINDER_BODY = "3초 안에 찰칵! 이번 주 나의 단골 매장 순위를 업데이트 해보세요.";
    private static final String FRIDAY_REMINDER_SCREEN = "/record/manual";

    private static final int FETCH_CHUNK_SIZE = 500;

    private final UserPushTargetPort userPushTargetPort;
    private final PushSenderPort pushSenderPort;
    private final NotificationRepository notificationRepository;

    // AFTER_COMMIT 리스너 안에서 호출되므로, 이전 트랜잭션 정리가 덜 끝난 상태에 편승하지 않도록 무조건 새 트랜잭션으로 강제
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReportCompleted(Long userId) {
        log.info("recordReportCompleted 진입. userId={}", userId);
        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.REPORT_COMPLETED)
                .title(REPORT_COMPLETED_TITLE)
                .body(REPORT_COMPLETED_BODY)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("recordReportCompleted 저장 완료. id={}, userId={}", saved.getId(), userId);
    }

    public void sendPendingReportCompletedPush() {
        List<Notification> pending = notificationRepository.findByTypeAndPushStatus(
                NotificationType.REPORT_COMPLETED, PushStatus.PENDING);

        if (pending.isEmpty()) {
            log.info("발송 대기 중인 리포트 완성 알림이 없습니다.");
            return;
        }

        List<Notification> sendable = new ArrayList<>();
        List<UserPushTarget> targets = new ArrayList<>();

        for (Notification notification : pending) {
            Optional<UserPushTarget> target = userPushTargetPort.findPushTarget(notification.getUserId());
            if (target.isPresent()) {
                sendable.add(notification);
                targets.add(target.get());
            } else {
                notification.markPushSkipped();
            }
        }

        if (!sendable.isEmpty()) {
            YearMonth reportMonth = YearMonth.from(LocalDate.now()).minusMonths(1);
            String screen = "/report/monthly-report?yearMonth=" + reportMonth;
            sendAndRecord(sendable, targets, screen);
        }
    }

    public void notifyFridayReminderToAll() {
        Long cursorId = 0L;

        while (true) {
            List<UserPushTarget> targets = userPushTargetPort.findActivePushTargets(cursorId, FETCH_CHUNK_SIZE);
            if (targets.isEmpty()) {
                break;
            }

            List<Notification> notifications = targets.stream()
                    .map(target -> Notification.builder()
                            .userId(target.userId())
                            .type(NotificationType.FRIDAY_REMINDER)
                            .title(FRIDAY_REMINDER_TITLE)
                            .body(FRIDAY_REMINDER_BODY)
                            .build())
                    .toList();
            notificationRepository.saveAll(notifications);

            sendAndRecord(notifications, targets, FRIDAY_REMINDER_SCREEN);

            cursorId = targets.get(targets.size() - 1).userId();
            if (targets.size() < FETCH_CHUNK_SIZE) {
                break;
            }
        }
    }

    // notifications와 targets는 같은 순서로 1:1 매칭된다고 가정
    private void sendAndRecord(List<Notification> notifications, List<UserPushTarget> targets, String screen) {
        List<String> tokens = targets.stream().map(UserPushTarget::fcmToken).toList();
        Notification representative = notifications.get(0);
        PushMessage message = new PushMessage(
                representative.getTitle(),
                representative.getBody(),
                Map.of("screen", screen)
        );

        PushSendResult result = pushSenderPort.sendMulticast(tokens, message);
        Set<String> invalidTokens = new HashSet<>(result.invalidTokens());

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            UserPushTarget target = targets.get(i);

            if (invalidTokens.contains(target.fcmToken())) {
                notification.markPushFailed();
                userPushTargetPort.invalidateToken(target.userId());
                log.info("무효 토큰 정리. userId={}", target.userId());
            } else {
                notification.markPushSent(null);
            }
        }
    }
}
