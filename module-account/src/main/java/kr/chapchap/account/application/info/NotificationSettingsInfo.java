package kr.chapchap.account.application.info;

import kr.chapchap.account.domain.entity.User;

public record NotificationSettingsInfo(
        boolean consumptionReminderEnabled,
        boolean monthlyReportEnabled
) {
    public static NotificationSettingsInfo from(User user) {
        return new NotificationSettingsInfo(
                user.isConsumptionReminderEnabled(),
                user.isMonthlyReportEnabled()
        );
    }
}
