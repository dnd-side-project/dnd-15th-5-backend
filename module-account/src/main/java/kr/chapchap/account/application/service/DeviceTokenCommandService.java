package kr.chapchap.account.application.service;

import kr.chapchap.account.application.command.RegisterDeviceTokenCommand;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Transactional
public class DeviceTokenCommandService {
    private final UserRepository userRepository;

    public void registerToken(RegisterDeviceTokenCommand command){
        User user = userRepository.findById(command.userId())
                .orElseThrow(()->new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        user.registerFcmToken(command.fcmToken(), LocalDateTime.now());
        userRepository.save(user);
    }

    public void unregisterToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        user.clearFcmToken();
        userRepository.save(user);
    }
}
