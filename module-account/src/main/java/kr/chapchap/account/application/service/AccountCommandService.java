package kr.chapchap.account.application.service;

import kr.chapchap.account.application.command.AccountUpdateCommand;
import kr.chapchap.account.application.event.ProfileImageCleanupEvent;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AccountCommandService {

    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;
    private final ProfileImageValidator profileImageValidator;
    private final ApplicationEventPublisher eventPublisher;

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

    private void validateCommand(AccountUpdateCommand command) {
        if (command.nickname() == null
                && !command.hasProfileImage()
                && !command.deleteProfileImage()) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_UPDATE_VALUE_REQUIRED);
        }
        if (command.hasProfileImage() && command.deleteProfileImage()) {
            throw new BusinessException(AccountErrorCode.PROFILE_IMAGE_UPDATE_CONFLICT);
        }
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
        if (!user.isActive()) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }
        return user;
    }

    private void updateNickname(User user, String nickname) {
        if (nickname == null) {
            return;
        }

        user.updateNickname(nickname);
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
