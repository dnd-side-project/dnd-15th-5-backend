package kr.chapchap.account;

import kr.chapchap.account.application.command.RegisterDeviceTokenCommand;
import kr.chapchap.account.application.info.NotificationSettingsInfo;
import kr.chapchap.account.application.service.AccountCommandService;
import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.account.application.service.DeviceTokenCommandService;
import kr.chapchap.account.application.service.PushTargetQueryService;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.notification.application.info.PushSendResult;
import kr.chapchap.notification.application.port.PushSenderPort;
import kr.chapchap.notification.application.service.PushNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationSettingsPersistenceIntegrationTest {

    @Autowired
    private AccountCommandService accountCommandService;
    @Autowired
    private AccountQueryService accountQueryService;
    @Autowired
    private DeviceTokenCommandService deviceTokenCommandService;
    @Autowired
    private PushTargetQueryService pushTargetQueryService;
    @Autowired
    private PushNotificationService pushNotificationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PushSenderPort pushSenderPort;

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void 알림함은_항상_유지하고_각_수신_설정에_맞는_푸시만_발송한다(
            boolean consumptionEnabled,
            boolean reportEnabled
    ) {
        // given
        String token = "ExponentPushToken[test-device]";
        User user = saveUser(true, token);
        accountCommandService.updateNotificationSettings(user.getId(), consumptionEnabled, reportEnabled);
        if (consumptionEnabled || reportEnabled) {
            when(pushSenderPort.sendMulticast(eq(List.of(token)), any()))
                    .thenReturn(new PushSendResult(1, 0, List.of()));
        }

        // when
        pushNotificationService.recordReportCompleted(user.getId());
        pushNotificationService.sendPendingReportCompletedPush();
        pushNotificationService.notifyFridayReminderToAll();

        // then
        assertThat(accountQueryService.getNotificationSettings(user.getId()))
                .isEqualTo(new NotificationSettingsInfo(consumptionEnabled, reportEnabled));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notifications", Long.class)).isEqualTo(2L);
        assertThat(findPushStatus("FRIDAY_REMINDER")).isEqualTo(consumptionEnabled ? "SENT" : "SKIPPED");
        assertThat(findPushStatus("REPORT_COMPLETED")).isEqualTo(reportEnabled ? "SENT" : "SKIPPED");
        int expectedCalls = (consumptionEnabled ? 1 : 0) + (reportEnabled ? 1 : 0);
        verify(pushSenderPort, times(expectedCalls)).sendMulticast(eq(List.of(token)), any());
    }

    @Test
    void 토큰이_없는_활성_사용자는_알림함만_받고_비활성_사용자는_대상에서_제외한다() {
        // given
        User active = saveUser(true, null);
        User pending = saveUser(false, "ExponentPushToken[pending-device]");

        // when
        pushNotificationService.recordReportCompleted(active.getId());
        pushNotificationService.sendPendingReportCompletedPush();
        pushNotificationService.notifyFridayReminderToAll();

        // then
        assertThat(pushTargetQueryService.findMonthlyReportPushTarget(active.getId())).isEmpty();
        assertThat(pushTargetQueryService.findMonthlyReportPushTarget(pending.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForList("SELECT user_id FROM notifications ORDER BY id", Long.class))
                .containsExactly(active.getId(), active.getId());
        assertThat(jdbcTemplate.queryForList("SELECT push_status FROM notifications", String.class))
                .containsOnly("SKIPPED");
        verify(pushSenderPort, times(0)).sendMulticast(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void 이전_계정의_탈퇴_여부와_관계없이_기기_토큰을_옮기고_같은_계정에_다시_등록할_수_있다(boolean withdrawn) {
        // given
        String token = "ExpoPushToken[shared-device]";
        User previous = saveUser(true, token);
        if (withdrawn) {
            previous.withdraw(LocalDateTime.now());
            userRepository.save(previous);
        }
        User current = saveUser(true, null);
        RegisterDeviceTokenCommand command = new RegisterDeviceTokenCommand(current.getId(), token);

        // when
        deviceTokenCommandService.registerToken(command);
        deviceTokenCommandService.registerToken(command);

        // then
        User previousAfterRegistration = userRepository.findById(previous.getId()).orElseThrow();
        assertThat(previousAfterRegistration.getPushToken()).isNull();
        assertThat(previousAfterRegistration.getPushTokenUpdatedAt()).isNull();
        User currentAfterRegistration = userRepository.findById(current.getId()).orElseThrow();
        assertThat(currentAfterRegistration.getPushToken()).isEqualTo(token);
        assertThat(currentAfterRegistration.getPushTokenUpdatedAt()).isNotNull();
    }

    private User saveUser(boolean active, String token) {
        User user = User.create("알림테스트");
        if (active) {
            user.completeTermsAgreement();
        }
        if (token != null) {
            user.registerPushToken(token, LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    private String findPushStatus(String type) {
        return jdbcTemplate.queryForObject(
                "SELECT push_status FROM notifications WHERE type = ?", String.class, type
        );
    }
}
