package kr.chapchap.notification.infra.external;

import kr.chapchap.account.application.info.PushTargetInfo;
import kr.chapchap.account.application.service.DeviceTokenCommandService;
import kr.chapchap.account.application.service.PushTargetQueryService;
import kr.chapchap.notification.application.info.UserPushTarget;
import kr.chapchap.notification.application.port.UserPushTargetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserPushTargetAdapter implements UserPushTargetPort {
    private final PushTargetQueryService pushTargetQueryService;
    private final DeviceTokenCommandService deviceTokenCommandService;

    @Override
    public Optional<UserPushTarget> findPushTarget(Long userId) {
        return pushTargetQueryService.findPushTarget(userId)
                .map(this::toUserPushTarget);
    }

    @Override
    public List<UserPushTarget> findActivePushTargets(Long cursorId, int limit) {
        return pushTargetQueryService.findActivePushTargets(cursorId, limit).stream()
                .map(this::toUserPushTarget)
                .toList();
    }

    @Override
    public void invalidateToken(Long userId) {
        deviceTokenCommandService.unregisterToken(userId);
    }

    private UserPushTarget toUserPushTarget(PushTargetInfo info) {
        return new UserPushTarget(info.userId(), info.fcmToken());
    }
}
