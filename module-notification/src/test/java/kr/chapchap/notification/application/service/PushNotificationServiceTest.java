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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserPushTargetPort userPushTargetPort;

    @Mock
    private PushSenderPort pushSenderPort;

    @Mock
    private NotificationRepository notificationRepository;

    private PushNotificationService sut;

    @BeforeEach
    void setUp() {
        sut = new PushNotificationService(userPushTargetPort, pushSenderPort, notificationRepository);
    }

    @Test
    void recordReportCompleted은_PENDING_상태의_알림을_저장한다() {
        // when
        sut.recordReportCompleted(USER_ID);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getType()).isEqualTo(NotificationType.REPORT_COMPLETED);
        assertThat(saved.getPushStatus()).isEqualTo(PushStatus.PENDING);
    }

    @Test
    void 토큰이_있는_유저는_발송_후_SENT로_표시되고_딥링크에_지난달이_담긴다() {
        // given
        Notification pending = reportCompletedNotification();
        when(notificationRepository.findByTypeAndPushStatus(NotificationType.REPORT_COMPLETED, PushStatus.PENDING))
                .thenReturn(List.of(pending));
        when(userPushTargetPort.findPushTarget(USER_ID))
                .thenReturn(Optional.of(new UserPushTarget(USER_ID, "token-1")));
        when(pushSenderPort.sendMulticast(eq(List.of("token-1")), any()))
                .thenReturn(new PushSendResult(1, 0, List.of()));

        // when
        sut.sendPendingReportCompletedPush();

        // then
        assertThat(pending.getPushStatus()).isEqualTo(PushStatus.SENT);
        verify(userPushTargetPort, never()).invalidateToken(any());

        ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSenderPort).sendMulticast(eq(List.of("token-1")), messageCaptor.capture());
        String expectedMonth = YearMonth.from(LocalDate.now()).minusMonths(1).toString();
        assertThat(messageCaptor.getValue().data().get("screen"))
                .isEqualTo("/report/monthly-report?yearMonth=" + expectedMonth);
    }

    @Test
    void 토큰이_없는_유저는_SKIPPED로_표시되고_발송_자체를_시도하지_않는다() {
        // given
        Notification pending = reportCompletedNotification();
        when(notificationRepository.findByTypeAndPushStatus(NotificationType.REPORT_COMPLETED, PushStatus.PENDING))
                .thenReturn(List.of(pending));
        when(userPushTargetPort.findPushTarget(USER_ID)).thenReturn(Optional.empty());

        // when
        sut.sendPendingReportCompletedPush();

        // then
        assertThat(pending.getPushStatus()).isEqualTo(PushStatus.SKIPPED);
        verify(pushSenderPort, never()).sendMulticast(any(), any());
    }

    @Test
    void 무효_토큰으로_실패하면_FAILED로_표시되고_토큰이_무효화된다() {
        // given
        Notification pending = reportCompletedNotification();
        when(notificationRepository.findByTypeAndPushStatus(NotificationType.REPORT_COMPLETED, PushStatus.PENDING))
                .thenReturn(List.of(pending));
        when(userPushTargetPort.findPushTarget(USER_ID))
                .thenReturn(Optional.of(new UserPushTarget(USER_ID, "dead-token")));
        when(pushSenderPort.sendMulticast(eq(List.of("dead-token")), any()))
                .thenReturn(new PushSendResult(0, 1, List.of("dead-token")));

        // when
        sut.sendPendingReportCompletedPush();

        // then
        assertThat(pending.getPushStatus()).isEqualTo(PushStatus.FAILED);
        verify(userPushTargetPort).invalidateToken(USER_ID);
    }

    @Test
    void 대기중인_알림이_없으면_아무것도_하지_않는다() {
        // given
        when(notificationRepository.findByTypeAndPushStatus(NotificationType.REPORT_COMPLETED, PushStatus.PENDING))
                .thenReturn(List.of());

        // when
        sut.sendPendingReportCompletedPush();

        // then
        verify(pushSenderPort, never()).sendMulticast(any(), any());
        verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    void 금요일_리마인더는_활성_대상_전체에게_한번에_발송하고_더_없으면_중단한다() {
        // given
        List<UserPushTarget> targets = List.of(
                new UserPushTarget(1L, "t1"),
                new UserPushTarget(2L, "t2")
        );
        when(userPushTargetPort.findActivePushTargets(eq(0L), anyInt())).thenReturn(targets);
        when(pushSenderPort.sendMulticast(eq(List.of("t1", "t2")), any()))
                .thenReturn(new PushSendResult(2, 0, List.of()));

        // when
        sut.notifyFridayReminderToAll();

        // then
        // 이번 청크(2명)가 조회 사이즈(500)보다 적으므로 다음 페이지는 조회하지 않고 종료
        verify(userPushTargetPort, times(1)).findActivePushTargets(any(), anyInt());
        verify(notificationRepository).saveAll(anyList());

        ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSenderPort).sendMulticast(eq(List.of("t1", "t2")), messageCaptor.capture());
        assertThat(messageCaptor.getValue().data().get("screen")).isEqualTo("/record/manual");
    }

    private Notification reportCompletedNotification() {
        return Notification.builder()
                .userId(USER_ID)
                .type(NotificationType.REPORT_COMPLETED)
                .title("이번 달 소비 취향이 완성됐어요!")
                .body("이번 달 가장 자주 찾은 동네와 새롭게 생긴 단골을 확인해 보세요.")
                .build();
    }
}
