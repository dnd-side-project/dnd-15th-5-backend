package kr.chapchap.account.application.event;

public record ProfileImageCleanupEvent(
        String newObjectKey,
        String previousObjectKey
) {
}
