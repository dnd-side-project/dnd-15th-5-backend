package kr.chapchap.notification.application.event;

import java.time.YearMonth;

public record ReportGeneratedEvent(Long userId, YearMonth reportMonth) {
}
