package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.PushTargetInfo;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PushTargetQueryService {
    private final UserRepository userRepository;

    public Optional<PushTargetInfo> findPushTarget(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .filter(user -> user.getFcmToken() != null && user.isPushEnabled())
                .map(user -> new PushTargetInfo(user.getId(), user.getFcmToken()));
    }

    public List<PushTargetInfo> findActivePushTargets(Long cursorId, int limit) {
        return userRepository.findActivePushTargets(cursorId, limit).stream()
                .map(user -> new PushTargetInfo(user.getId(), user.getFcmToken()))
                .toList();
    }
}
