package kr.chapchap.notification.application.info;

import java.util.List;

public record PushSendResult(int successCount, int failureCount, List<String> invalidTokens, List<String> failedTokens) {
    public PushSendResult(int successCount, int failureCount, List<String> invalidTokens) {
        this(successCount, failureCount, invalidTokens, List.of());
    }
}
