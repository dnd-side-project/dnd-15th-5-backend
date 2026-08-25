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
import kr.chapchap.account.domain.entity.UserStatus;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalServiceTest {

    private static final Long USER_ID = 1L;
    private static final String GOOGLE_SUB = "google-sub";
    private static final String WITHDRAWAL_STATE = "withdrawal-state";
    private static final String AUTHORIZATION_CODE = "authorization-code";
    private static final String ACCESS_TOKEN = "google-access-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private KakaoAuthenticationPort kakaoAuthenticationPort;

    @Mock
    private GoogleAuthenticationPort googleAuthenticationPort;

    @Mock
    private OAuthSessionStore oauthSessionStore;

    @Mock
    private OAuthClientRedirectPort oauthClientRedirectPort;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AccountWithdrawalService accountWithdrawalService;

    @Test
    void Google_회원은_탈퇴_재인증_URI를_받는다() {
        // given
        User user = createActiveUser();
        SocialAccount googleAccount = createGoogleAccount();
        URI authorizationUri = URI.create(
                "https://accounts.google.com/o/oauth2/v2/auth?state=" + WITHDRAWAL_STATE
        );
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.KAKAO
        )).willReturn(Optional.empty());
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.GOOGLE
        )).willReturn(Optional.of(googleAccount));
        given(oauthSessionStore.createWithdrawalState(USER_ID, OAuthClientType.WEB))
                .willReturn(WITHDRAWAL_STATE);
        given(googleAuthenticationPort.createReauthenticationUri(WITHDRAWAL_STATE))
                .willReturn(authorizationUri);

        // when
        Optional<URI> result = accountWithdrawalService.startWithdrawal(
                USER_ID,
                OAuthClientType.WEB
        );

        // then
        assertThat(result).contains(authorizationUri);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        then(googleAuthenticationPort).should()
                .createReauthenticationUri(WITHDRAWAL_STATE);
        then(googleAuthenticationPort).shouldHaveNoMoreInteractions();
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    @Test
    void 같은_Google_sub로_재인증하면_권한을_해제하고_탈퇴한다() {
        // given
        User user = createActiveUser();
        URI redirectUri = URI.create("chapchap://oauth/callback?withdrawal=success");
        givenWithdrawalSession();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.GOOGLE
        )).willReturn(Optional.of(createGoogleAccount()));
        given(googleAuthenticationPort.authenticateForWithdrawal(
                AUTHORIZATION_CODE,
                WITHDRAWAL_STATE
        )).willReturn(new GoogleWithdrawalAuthenticationInfo(GOOGLE_SUB, ACCESS_TOKEN));
        given(oauthClientRedirectPort.createWithdrawalRedirect(OAuthClientType.APP))
                .willReturn(redirectUri);

        // when
        AccountWithdrawalCallbackInfo result =
                accountWithdrawalService.handleGoogleCallback(
                        AUTHORIZATION_CODE,
                        WITHDRAWAL_STATE
                );

        // then
        assertThat(result).isEqualTo(new AccountWithdrawalCallbackInfo(redirectUri, true));
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getWithdrawnAt()).isNotNull();
        InOrder inOrder = inOrder(googleAuthenticationPort, refreshTokenStore);
        inOrder.verify(googleAuthenticationPort).authenticateForWithdrawal(
                AUTHORIZATION_CODE,
                WITHDRAWAL_STATE
        );
        inOrder.verify(googleAuthenticationPort).revoke(ACCESS_TOKEN);
        inOrder.verify(refreshTokenStore).revokeAll(USER_ID);
    }

    @Test
    void 다른_Google_sub로_재인증하면_탈퇴하지_않는다() {
        // given
        User user = createActiveUser();
        URI errorRedirectUri = URI.create(
                "chapchap://oauth/callback?error=withdrawal_failed"
        );
        givenWithdrawalSession();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.GOOGLE
        )).willReturn(Optional.of(createGoogleAccount()));
        given(googleAuthenticationPort.authenticateForWithdrawal(
                AUTHORIZATION_CODE,
                WITHDRAWAL_STATE
        )).willReturn(new GoogleWithdrawalAuthenticationInfo("other-sub", ACCESS_TOKEN));
        given(oauthClientRedirectPort.createErrorRedirect(
                OAuthClientType.APP,
                "withdrawal_failed"
        )).willReturn(errorRedirectUri);

        // when
        AccountWithdrawalCallbackInfo result =
                accountWithdrawalService.handleGoogleCallback(
                        AUTHORIZATION_CODE,
                        WITHDRAWAL_STATE
                );

        // then
        assertThat(result).isEqualTo(new AccountWithdrawalCallbackInfo(
                errorRedirectUri,
                false
        ));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        then(googleAuthenticationPort).should().authenticateForWithdrawal(
                AUTHORIZATION_CODE,
                WITHDRAWAL_STATE
        );
        then(googleAuthenticationPort).shouldHaveNoMoreInteractions();
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    @Test
    void Google_권한_해제에_실패하면_탈퇴와_토큰_정리를_진행하지_않는다() {
        // given
        User user = createActiveUser();
        URI errorRedirectUri = URI.create(
                "chapchap://oauth/callback?error=withdrawal_failed"
        );
        givenWithdrawalSession();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.GOOGLE
        )).willReturn(Optional.of(createGoogleAccount()));
        given(googleAuthenticationPort.authenticateForWithdrawal(
                AUTHORIZATION_CODE,
                WITHDRAWAL_STATE
        )).willReturn(new GoogleWithdrawalAuthenticationInfo(GOOGLE_SUB, ACCESS_TOKEN));
        willThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE))
                .given(googleAuthenticationPort)
                .revoke(ACCESS_TOKEN);
        given(oauthClientRedirectPort.createErrorRedirect(
                OAuthClientType.APP,
                "withdrawal_failed"
        )).willReturn(errorRedirectUri);

        // when
        AccountWithdrawalCallbackInfo result =
                accountWithdrawalService.handleGoogleCallback(
                        AUTHORIZATION_CODE,
                        WITHDRAWAL_STATE
                );

        // then
        assertThat(result.completed()).isFalse();
        assertThat(result.redirectUri()).isEqualTo(errorRedirectUri);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    @Test
    void 탈퇴_회원_ID를_순서대로_조회한다() {
        // given
        given(userRepository.findWithdrawnUserIds())
                .willReturn(List.of(1L, 3L));

        // when
        List<Long> result = accountWithdrawalService.findWithdrawnUserIds();

        // then
        assertThat(result).containsExactly(1L, 3L);
    }

    @Test
    void WITHDRAWN_상태의_회원을_물리_삭제한다() {
        // given
        User user = createActiveUser();
        user.withdraw(LocalDateTime.now());
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        // when
        boolean result = accountWithdrawalService.deleteWithdrawnUser(USER_ID);

        // then
        assertThat(result).isTrue();
        then(userRepository).should().delete(user);
    }

    @Test
    void ACTIVE_상태의_회원은_물리_삭제하지_않는다() {
        // given
        User user = createActiveUser();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        // when
        boolean result = accountWithdrawalService.deleteWithdrawnUser(USER_ID);

        // then
        assertThat(result).isFalse();
        then(userRepository).should().findByIdForUpdate(USER_ID);
        then(userRepository).shouldHaveNoMoreInteractions();
    }

    private void givenWithdrawalSession() {
        given(oauthSessionStore.consumeWithdrawalState(WITHDRAWAL_STATE))
                .willReturn(Optional.of(new OAuthWithdrawalSession(
                        USER_ID,
                        OAuthClientType.APP
                )));
    }

    private User createActiveUser() {
        User user = User.create("테스트회원");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeTermsAgreement();
        return user;
    }

    private SocialAccount createGoogleAccount() {
        return SocialAccount.create(USER_ID, SocialProvider.GOOGLE, GOOGLE_SUB);
    }
}
