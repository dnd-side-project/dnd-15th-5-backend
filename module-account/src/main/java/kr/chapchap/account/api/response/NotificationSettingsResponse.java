package kr.chapchap.account.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.account.application.info.NotificationSettingsInfo;

@Schema(description = "알림 수신 설정. 스마트폰의 OS 알림 권한과는 별개입니다.")
public record NotificationSettingsResponse(
        @Schema(description = "소비 기록 푸시 알림 수신 여부")
        boolean consumptionReminderEnabled,

        @Schema(description = "월간 리포트 푸시 알림 수신 여부")
        boolean monthlyReportEnabled
) {
    public static NotificationSettingsResponse from(NotificationSettingsInfo info) {
        return new NotificationSettingsResponse(
                info.consumptionReminderEnabled(),
                info.monthlyReportEnabled()
        );
    }
}
