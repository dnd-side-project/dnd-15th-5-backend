package kr.chapchap.notification.application.port;

import kr.chapchap.notification.application.info.UserPushTarget;
import kr.chapchap.notification.application.info.ReminderRecipient;

import java.util.List;
import java.util.Optional;

public interface UserPushTargetPort {
    Optional<UserPushTarget> findMonthlyReportPushTarget(Long userId);

    List<ReminderRecipient> findReminderRecipients(Long cursorId, int limit);

    void invalidateToken(Long userId);
}
