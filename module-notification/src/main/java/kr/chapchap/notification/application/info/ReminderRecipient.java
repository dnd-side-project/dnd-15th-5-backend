package kr.chapchap.notification.application.info;

public record ReminderRecipient(
        Long userId,
        String pushToken,
        boolean consumptionReminderEnabled
) {
    public boolean canReceivePush() {
        return consumptionReminderEnabled && pushToken != null && !pushToken.isBlank();
    }
}
