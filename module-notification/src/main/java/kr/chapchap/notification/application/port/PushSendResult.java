package kr.chapchap.notification.application.port;

import java.util.List;

public record PushSendResult(int successCount, int failureCount, List<String> invalidTokens) {
}
