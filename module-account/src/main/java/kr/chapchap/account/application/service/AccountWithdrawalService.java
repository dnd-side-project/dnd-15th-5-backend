package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AccountWithdrawalCallbackInfo;
import kr.chapchap.account.application.info.GoogleWithdrawalAuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthWithdrawalSession;
import kr.chapchap.account.application.port.GoogleAuthenticationPort;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.OAuthClientRedirectPort;
import kr.chapchap.account.application.port.OAuthSessionStore;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountWithdrawalService {

    private static final String GOOGLE_PROVIDER = "google";
    private static final String WITHDRAWAL_CANCELLED = "withdrawal_cancelled";
    private static final String WITHDRAWAL_FAILED = "withdrawal_failed";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final KakaoAuthenticationPort kakaoAuthenticationPort;
    private final GoogleAuthenticationPort googleAuthenticationPort;
    private final OAuthSessionStore oauthSessionStore;
    private final OAuthClientRedirectPort oauthClientRedirectPort;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public Optional<URI> startWithdrawal(Long userId, OAuthClientType clientType) {
        User user = getActiveUser(userId);
        SocialAccount socialAccount = getSocialAccount(userId);

        if (socialAccount.getProvider() == SocialProvider.KAKAO) {
            kakaoAuthenticationPort.unlink(socialAccount.getProviderUserId());
            completeWithdrawal(user);
            return Optional.empty();
        }

        String state = oauthSessionStore.createWithdrawalState(userId, clientType);
        return Optional.of(googleAuthenticationPort.createReauthenticationUri(state));
    }

    public boolean isGoogleWithdrawalCallback(String provider, String state) {
        return GOOGLE_PROVIDER.equals(provider)
                && state != null
                && state.startsWith(OAuthWithdrawalSession.STATE_PREFIX);
    }

    @Transactional
    public AccountWithdrawalCallbackInfo handleGoogleCallback(
            String authorizationCode,
            String state
    ) {
        OAuthWithdrawalSession session = consumeWithdrawalSession(state);
        try {
            User user = getActiveUser(session.userId());
            SocialAccount googleAccount = socialAccountRepository
                    .findByUserIdAndProvider(session.userId(), SocialProvider.GOOGLE)
                    .orElseThrow(() -> new BusinessException(
                            CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                    ));
            GoogleWithdrawalAuthenticationInfo authenticationInfo =
                    googleAuthenticationPort.authenticateForWithdrawal(
                            authorizationCode,
                            state
                    );
            if (!googleAccount.getProviderUserId().equals(
                    authenticationInfo.providerUserId()
            )) {
                throw new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                );
            }

            googleAuthenticationPort.revoke(authenticationInfo.accessToken());
            completeWithdrawal(user);
            return new AccountWithdrawalCallbackInfo(
                    oauthClientRedirectPort.createWithdrawalRedirect(session.clientType()),
                    true
            );
        } catch (BusinessException exception) {
            log.warn(
                    "Google 회원 탈퇴 처리 실패: code={}",
                    exception.getErrorCode().getCode()
            );
            return new AccountWithdrawalCallbackInfo(
                    oauthClientRedirectPort.createErrorRedirect(
                            session.clientType(),
                            WITHDRAWAL_FAILED
                    ),
                    false
            );
        }
    }

    public AccountWithdrawalCallbackInfo handleGoogleCancelledCallback(String state) {
        OAuthWithdrawalSession session = consumeWithdrawalSession(state);
        return new AccountWithdrawalCallbackInfo(
                oauthClientRedirectPort.createErrorRedirect(
                        session.clientType(),
                        WITHDRAWAL_CANCELLED
                ),
                false
        );
    }

    @Transactional(readOnly = true)
    public List<Long> findWithdrawnUserIds() {
        return userRepository.findWithdrawnUserIds();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteWithdrawnUser(Long userId) {
        Optional<User> user = userRepository.findByIdForUpdate(userId);
        if (user.isEmpty() || !user.get().isWithdrawn()) {
            return false;
        }

        userRepository.delete(user.get());
        return true;
    }

    private OAuthWithdrawalSession consumeWithdrawalSession(String state) {
        return oauthSessionStore.consumeWithdrawalState(state)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
    }

    private SocialAccount getSocialAccount(Long userId) {
        Optional<SocialAccount> kakaoAccount = socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.KAKAO);
        if (kakaoAccount.isPresent()) {
            return kakaoAccount.get();
        }
        return socialAccountRepository.findByUserIdAndProvider(
                        userId,
                        SocialProvider.GOOGLE
                )
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
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

    private void completeWithdrawal(User user) {
        refreshTokenStore.revokeAll(user.getId());
        user.withdraw(LocalDateTime.now());
    }
}
