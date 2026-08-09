package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthAuthorizationSession;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthLoginSession;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.OAuthClientRedirectPort;
import kr.chapchap.account.application.port.OAuthSessionStore;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class KakaoOAuthFlowServiceTest {

    private static final String CODE_CHALLENGE =
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String CODE_VERIFIER =
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    @Mock
    private KakaoAuthenticationPort kakaoAuthenticationPort;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private OAuthSessionStore oauthSessionStore;

    @Mock
    private OAuthClientRedirectPort oauthClientRedirectPort;

    @Mock
    private LoginTokenService loginTokenService;

    @InjectMocks
    private KakaoOAuthFlowService kakaoOAuthFlowService;

    @Test
    void 콜백에서_OAuth_state를_소비하고_loginCode를_담은_클라이언트_URI를_반환한다() {
        // given
        URI redirectUri = URI.create("chapchap://oauth/callback?loginCode=login-code");

        given(oauthSessionStore.consumeState("state"))
                .willReturn(Optional.of(new OAuthAuthorizationSession(
                        OAuthClientType.APP,
                        CODE_CHALLENGE
                )));
        given(kakaoAuthenticationPort.authenticate("authorization-code"))
                .willReturn("123456789");
        given(socialLoginService.login(SocialProvider.KAKAO, "123456789"))
                .willReturn(1L);
        given(oauthSessionStore.createLoginCode(
                1L,
                OAuthClientType.APP,
                CODE_CHALLENGE
        ))
                .willReturn("login-code");
        given(oauthClientRedirectPort.createLoginRedirect(
                OAuthClientType.APP,
                "login-code"
        )).willReturn(redirectUri);

        // when
        URI result = kakaoOAuthFlowService.handleCallback("authorization-code", "state");

        // then
        assertThat(result).isEqualTo(redirectUri);

        InOrder inOrder = inOrder(
                oauthSessionStore,
                kakaoAuthenticationPort,
                socialLoginService,
                oauthClientRedirectPort
        );
        inOrder.verify(oauthSessionStore).consumeState("state");
        inOrder.verify(kakaoAuthenticationPort).authenticate("authorization-code");
        inOrder.verify(socialLoginService).login(SocialProvider.KAKAO, "123456789");
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
    void 올바른_codeVerifier로_loginCode를_교환한다() {
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
        AuthenticationInfo result = kakaoOAuthFlowService.exchange(
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
                OAuthClientType.WEB,
                CODE_CHALLENGE
        );
        URI redirectUri = URI.create("https://web.example.com/oauth/callback?error=oauth_failed");

        given(oauthSessionStore.consumeState("state"))
                .willReturn(Optional.of(authorizationSession));
        given(kakaoAuthenticationPort.authenticate("authorization-code"))
                .willThrow(new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));
        given(oauthClientRedirectPort.createErrorRedirect(
                OAuthClientType.WEB,
                "oauth_failed"
        )).willReturn(redirectUri);

        // when
        URI result = kakaoOAuthFlowService.handleCallback("authorization-code", "state");

        // then
        assertThat(result).isEqualTo(redirectUri);
        then(socialLoginService).shouldHaveNoInteractions();
        then(oauthClientRedirectPort).should().createErrorRedirect(
                OAuthClientType.WEB,
                "oauth_failed"
        );
    }
}
