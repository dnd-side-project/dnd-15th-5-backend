package kr.chapchap.notification.application.port;

import kr.chapchap.notification.application.info.UserPushTarget;

import java.util.List;
import java.util.Optional;

public interface UserPushTargetPort {
    Optional<UserPushTarget> findPushTarget(Long userId);

    List<UserPushTarget> findActivePushTargets(Long cursorId, int limit);

    void invalidateToken(Long userId);
}
