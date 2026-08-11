package kr.chapchap.account.application.service;

import kr.chapchap.account.application.command.AccountUpdateCommand;
import kr.chapchap.account.application.event.ProfileImageCleanupEvent;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AccountCommandService {

    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;
    private final ProfileImageValidator profileImageValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final SocialAccountRepository socialAccountRepository;
    private final KakaoAuthenticationPort kakaoAuthenticationPort;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public AccountInfo updateAccount(AccountUpdateCommand command) {
        validateCommand(command);
        String profileImageContentType = command.hasProfileImage()
                ? profileImageValidator.validateAndGetContentType(command.profileImageContent())
                : null;
        User user = getActiveUser(command.userId());

        updateNickname(user, command.nickname());
        updateProfileImage(user, command, profileImageContentType);

        String profileImageUrl = user.getProfileImageKey() == null
                ? null
                : profileImageStorage.createReadUrl(user.getProfileImageKey());
        return AccountInfo.from(user, profileImageUrl);
    }

    @Transactional
    public void withdrawAccount(Long userId) {
        User user = getActiveUser(userId);

        SocialAccount kakaoAccount = socialAccountRepository.findByUserIdAndProvider(
                        userId,
                        SocialProvider.KAKAO
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));

        kakaoAuthenticationPort.unlink(kakaoAccount.getProviderUserId());
        refreshTokenStore.revokeAll(userId);
        user.withdraw(LocalDateTime.now());
    }

    private void validateCommand(AccountUpdateCommand command) {
        if (command.nickname() == null
                && !command.hasProfileImage()
                && !command.deleteProfileImage()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (command.hasProfileImage() && command.deleteProfileImage()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return user;
    }

    private void updateNickname(User user, String nickname) {
        if (nickname == null) {
            return;
        }

        try {
            user.updateNickname(nickname);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, exception);
        }
    }

    private void updateProfileImage(
            User user,
            AccountUpdateCommand command,
            String contentType
    ) {
        if (command.hasProfileImage()) {
            String previousObjectKey = user.getProfileImageKey();
            String objectKey = profileImageStorage.store(
                    user.getId(),
                    command.profileImageContent(),
                    contentType
            );
            eventPublisher.publishEvent(new ProfileImageCleanupEvent(
                    objectKey,
                    previousObjectKey
            ));
            user.updateProfileImageKey(objectKey);
            return;
        }

        if (command.deleteProfileImage()) {
            String previousObjectKey = user.getProfileImageKey();
            user.deleteProfileImage();
            if (previousObjectKey != null) {
                eventPublisher.publishEvent(new ProfileImageCleanupEvent(
                        null,
                        previousObjectKey
                ));
            }
        }
    }

}
