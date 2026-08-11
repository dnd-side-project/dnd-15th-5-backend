package kr.chapchap.account.infra.event;

import kr.chapchap.account.application.event.ProfileImageCleanupEvent;
import kr.chapchap.account.application.port.ProfileImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileImageCleanupListener {

    private final ProfileImageStorage profileImageStorage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deletePreviousImage(ProfileImageCleanupEvent event) {
        deleteSafely(event.previousObjectKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteRolledBackImage(ProfileImageCleanupEvent event) {
        deleteSafely(event.newObjectKey());
    }

    private void deleteSafely(String objectKey) {
        if (objectKey == null) {
            return;
        }

        try {
            profileImageStorage.delete(objectKey);
        } catch (RuntimeException exception) {
            log.error("프로필 이미지 정리에 실패했습니다.", exception);
        }
    }
}
