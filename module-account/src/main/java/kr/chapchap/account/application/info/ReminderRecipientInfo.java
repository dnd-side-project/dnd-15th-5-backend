package kr.chapchap.account.application.info;

public record ReminderRecipientInfo(
        Long userId,
        String pushToken,
        boolean consumptionReminderEnabled
) {
}
