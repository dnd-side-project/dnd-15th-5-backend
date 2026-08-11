package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AccountQueryService {

    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;

    public AccountInfo getAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String profileImageUrl = user.getProfileImageKey() == null
                ? null
                : profileImageStorage.createReadUrl(user.getProfileImageKey());

        return AccountInfo.from(user, profileImageUrl);
    }
}
