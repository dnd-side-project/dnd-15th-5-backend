package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthAuthorizationSession;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthLoginSession;
import kr.chapchap.account.application.port.GoogleAuthenticationPort;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.OAuthClientRedirectPort;
import kr.chapchap.account.application.port.OAuthSessionStore;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class OAuthFlowServiceTest {

    private static final String CODE_CHALLENGE =
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String CODE_VERIFIER =
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    @Mock
    private KakaoAuthenticationPort kakaoAuthenticationPort;

    @Mock
    private GoogleAuthenticationPort googleAuthenticationPort;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private OAuthSessionStore oauthSessionStore;

    @Mock
    private OAuthClientRedirectPort oauthClientRedirectPort;

    @Mock
    private LoginTokenService loginTokenService;

    private OAuthFlowService oauthFlowService;

    @BeforeEach
    void setUp() {
        oauthFlowService = new OAuthFlowService(
                kakaoAuthenticationPort,
                googleAuthenticationPort,
                socialLoginService,
                oauthSessionStore,
                oauthClientRedirectPort,
                loginTokenService
        );
    }

    @Test
    void Google_로컬_WEB_로그인을_시작하면_클라이언트가_묶인_state로_인가_URI를_생성한다() {
        // given
        URI authorizationUri = URI.create("https://accounts.google.com/o/oauth2/v2/auth");
        given(oauthSessionStore.createState(
                SocialProvider.GOOGLE,
                OAuthClientType.WEB_LOCAL,
                CODE_CHALLENGE
        )).willReturn("google-state");
        given(googleAuthenticationPort.createAuthorizationUri("google-state"))
                .willReturn(authorizationUri);

        // when
        URI result = oauthFlowService.createAuthorizationUri(
                "google",
                OAuthClientType.WEB_LOCAL,
                CODE_CHALLENGE
        );

        // then
        assertThat(result).isEqualTo(authorizationUri);
    }

    @Test
    void Google_콜백에서_state를_소비하고_sub로_loginCode를_생성한다() {
        // given
        URI redirectUri = URI.create("chapchap://oauth/callback?loginCode=login-code");
        given(oauthSessionStore.consumeState("google-state"))
                .willReturn(Optional.of(new OAuthAuthorizationSession(
                        SocialProvider.GOOGLE,
                        OAuthClientType.APP,
                        CODE_CHALLENGE
                )));
        given(googleAuthenticationPort.authenticate("authorization-code", "google-state"))
                .willReturn("google-sub");
        given(socialLoginService.login(SocialProvider.GOOGLE, "google-sub"))
                .willReturn(1L);
        given(oauthSessionStore.createLoginCode(
                1L,
                OAuthClientType.APP,
                CODE_CHALLENGE
        )).willReturn("login-code");
        given(oauthClientRedirectPort.createLoginRedirect(
                OAuthClientType.APP,
                "login-code"
        )).willReturn(redirectUri);

        // when
        URI result = oauthFlowService.handleCallback(
                "google",
                "authorization-code",
                "google-state"
        );

        // then
        assertThat(result).isEqualTo(redirectUri);
        InOrder inOrder = inOrder(
                oauthSessionStore,
                googleAuthenticationPort,
                socialLoginService,
                oauthClientRedirectPort
        );
        inOrder.verify(oauthSessionStore).consumeState("google-state");
        inOrder.verify(googleAuthenticationPort)
                .authenticate("authorization-code", "google-state");
        inOrder.verify(socialLoginService).login(SocialProvider.GOOGLE, "google-sub");
        inOrder.verify(oauthSessionStore).createLoginCode(
                1L,
                OAuthClientType.APP,
                CODE_CHALLENGE
        );
        inOrder.verify(oauthClientRedirectPort).createLoginRedirect(
                OAuthClientType.APP,
                "login-code"
        );
    }

    @Test
    void 카카오_콜백에서_state를_소비하고_Provider_사용자_ID로_loginCode를_생성한다() {
        // given
        URI redirectUri = URI.create(
                "http://localhost:5173/auth/callback?loginCode=login-code"
        );
        given(oauthSessionStore.consumeState("kakao-state"))
                .willReturn(Optional.of(new OAuthAuthorizationSession(
                        SocialProvider.KAKAO,
                        OAuthClientType.WEB_LOCAL,
                        CODE_CHALLENGE
                )));
        given(kakaoAuthenticationPort.authenticate("authorization-code"))
                .willReturn("123456789");
        given(socialLoginService.login(SocialProvider.KAKAO, "123456789"))
                .willReturn(1L);
        given(oauthSessionStore.createLoginCode(
                1L,
                OAuthClientType.WEB,
                CODE_CHALLENGE
        )).willReturn("login-code");
        given(oauthClientRedirectPort.createLoginRedirect(
                OAuthClientType.WEB_LOCAL,
                "login-code"
        )).willReturn(redirectUri);

        // when
        URI result = oauthFlowService.handleCallback(
                "kakao",
                "authorization-code",
                "kakao-state"
        );

        // then
        assertThat(result).isEqualTo(redirectUri);
        InOrder inOrder = inOrder(
                oauthSessionStore,
                kakaoAuthenticationPort,
                socialLoginService,
                oauthClientRedirectPort
        );
        inOrder.verify(oauthSessionStore).consumeState("kakao-state");
        inOrder.verify(kakaoAuthenticationPort).authenticate("authorization-code");
        inOrder.verify(socialLoginService).login(SocialProvider.KAKAO, "123456789");
        inOrder.verify(oauthSessionStore).createLoginCode(
                1L,
                OAuthClientType.WEB,
                CODE_CHALLENGE
        );
        inOrder.verify(oauthClientRedirectPort).createLoginRedirect(
                OAuthClientType.WEB_LOCAL,
                "login-code"
        );
        then(googleAuthenticationPort).shouldHaveNoInteractions();
    }

    @Test
    void 콜백_Provider와_state의_Provider가_다르면_인증을_거부한다() {
        // given
        given(oauthSessionStore.consumeState("kakao-state"))
                .willReturn(Optional.of(new OAuthAuthorizationSession(
                        SocialProvider.KAKAO,
                        OAuthClientType.WEB,
                        CODE_CHALLENGE
                )));

        // when & then
        assertThatThrownBy(() -> oauthFlowService.handleCallback(
                "google",
                "authorization-code",
                "kakao-state"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
        then(googleAuthenticationPort).shouldHaveNoMoreInteractions();
        then(socialLoginService).shouldHaveNoInteractions();
    }

    @Test
    void 올바른_codeVerifier로_공통_loginCode를_교환한다() {
        // given
        AuthenticationInfo authenticationInfo = AuthenticationInfo.termsRequired(
                OAuthClientType.APP,
                "signup-token"
        );
        given(oauthSessionStore.consumeLoginCode("login-code", CODE_CHALLENGE))
                .willReturn(Optional.of(new OAuthLoginSession(
                        1L,
                        OAuthClientType.APP
                )));
        given(loginTokenService.issueForLogin(1L, OAuthClientType.APP))
                .willReturn(authenticationInfo);

        // when
        AuthenticationInfo result = oauthFlowService.exchange(
                "login-code",
                CODE_VERIFIER
        );

        // then
        assertThat(result).isEqualTo(authenticationInfo);
        then(oauthSessionStore).should().consumeLoginCode("login-code", CODE_CHALLENGE);
        then(loginTokenService).should().issueForLogin(1L, OAuthClientType.APP);
    }

    @Test
    void 카카오_인증에_실패해도_error가_포함된_클라이언트_URI로_복귀한다() {
        // given
        OAuthAuthorizationSession authorizationSession = new OAuthAuthorizationSession(
                SocialProvider.KAKAO,
                OAuthClientType.WEB_LOCAL,
                CODE_CHALLENGE
        );
        URI redirectUri = URI.create(
                "http://localhost:5173/auth/callback?error=oauth_failed"
        );
        given(oauthSessionStore.consumeState("kakao-state"))
                .willReturn(Optional.of(authorizationSession));
        given(kakaoAuthenticationPort.authenticate("authorization-code"))
                .willThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));
        given(oauthClientRedirectPort.createErrorRedirect(
                OAuthClientType.WEB_LOCAL,
                "oauth_failed"
        )).willReturn(redirectUri);

        // when
        URI result = oauthFlowService.handleCallback(
                "kakao",
                "authorization-code",
                "kakao-state"
        );

        // then
        assertThat(result).isEqualTo(redirectUri);
        then(socialLoginService).shouldHaveNoInteractions();
        then(oauthClientRedirectPort).should().createErrorRedirect(
                OAuthClientType.WEB_LOCAL,
                "oauth_failed"
        );
    }

    @Test
    void 탈퇴한_계정의_OAuth_콜백은_탈퇴_계정_error가_포함된_클라이언트_URI로_복귀한다() {
        // given
        OAuthAuthorizationSession authorizationSession = new OAuthAuthorizationSession(
                SocialProvider.GOOGLE,
                OAuthClientType.WEB_LOCAL,
                CODE_CHALLENGE
        );
        URI redirectUri = URI.create(
                "http://localhost:5173/auth/callback?error=account_withdrawn"
        );
        given(oauthSessionStore.consumeState("google-state"))
                .willReturn(Optional.of(authorizationSession));
        given(googleAuthenticationPort.authenticate("authorization-code", "google-state"))
                .willReturn("google-sub");
        given(socialLoginService.login(SocialProvider.GOOGLE, "google-sub"))
                .willThrow(new BusinessException(AccountErrorCode.ACCOUNT_WITHDRAWN));
        given(oauthClientRedirectPort.createErrorRedirect(
                OAuthClientType.WEB_LOCAL,
                "account_withdrawn"
        )).willReturn(redirectUri);

        // when
        URI result = oauthFlowService.handleCallback(
                "google",
                "authorization-code",
                "google-state"
        );

        // then
        assertThat(result).isEqualTo(redirectUri);
        then(oauthClientRedirectPort).should().createErrorRedirect(
                OAuthClientType.WEB_LOCAL,
                "account_withdrawn"
        );
        then(loginTokenService).shouldHaveNoInteractions();
    }
}
