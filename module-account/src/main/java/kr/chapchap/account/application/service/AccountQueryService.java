package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AccountQueryService {

    private final UserRepository userRepository;

    public AccountInfo getAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS));

        if (!user.isActive()) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        return AccountInfo.from(user, null);
    }

    public Optional<String> getNickname(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .map(User::getNickname);
    }
}
