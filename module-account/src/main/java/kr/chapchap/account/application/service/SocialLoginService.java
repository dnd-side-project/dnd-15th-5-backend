package kr.chapchap.account.application.service;

import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.domain.service.NicknameGenerator;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class SocialLoginService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final NicknameGenerator nicknameGenerator;

    @Transactional
    public Long login(
            SocialProvider provider,
            String providerUserId
    ) {
        Objects.requireNonNull(provider, "소셜 로그인 제공자는 필수입니다.");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 로그인 사용자 식별값은 비어 있을 수 없습니다.");
        }

        return findOrCreate(provider, providerUserId);
    }

    private Long findOrCreate(
            SocialProvider provider,
            String providerUserId
    ) {
        return socialAccountRepository.findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                )
                .map(SocialAccount::getUserId)
                .map(this::findUser)
                .map(this::toUserId)
                .orElseGet(() -> createUserAndSocialAccount(provider, providerUserId));
    }

    private Long createUserAndSocialAccount(
            SocialProvider provider,
            String providerUserId
    ) {
        User user = userRepository.save(User.create(nicknameGenerator.generate()));

        SocialAccount socialAccount = SocialAccount.create(
                user.getId(),
                provider,
                providerUserId
        );
        socialAccountRepository.save(socialAccount);

        return toUserId(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
    }

    private Long toUserId(User user) {
        return switch (user.getStatus()) {
            case PENDING_TERMS, ACTIVE -> user.getId();
            case SUSPENDED, WITHDRAWN -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        };
    }

}
