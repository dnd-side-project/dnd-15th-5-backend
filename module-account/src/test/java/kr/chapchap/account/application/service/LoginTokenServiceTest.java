package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.RefreshTokenClaims;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.application.port.TokenProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LoginTokenServiceTest {

    private static final Long USER_ID = 1L;
    private static final String OLD_REFRESH_TOKEN_ID = "old-refresh-token-id";
    private static final String NEW_REFRESH_TOKEN_ID = "new-refresh-token-id";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private LoginTokenService loginTokenService;

    @Test
    void 약관_동의_대기_사용자에게는_Signup_Token만_발급한다() {
        // given
        User pendingUser = User.create("참참이");
        String signupToken = "signup-token";

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(pendingUser));
        given(tokenProvider.issueSignupToken(USER_ID, OAuthClientType.WEB))
                .willReturn(signupToken);

        // when
        AuthenticationInfo result = loginTokenService.issueForLogin(
                USER_ID,
                OAuthClientType.WEB
        );

        // then
        assertThat(result).isEqualTo(AuthenticationInfo.termsRequired(
                OAuthClientType.WEB,
                signupToken
        ));
        then(tokenProvider).should().issueSignupToken(USER_ID, OAuthClientType.WEB);
        then(tokenProvider).should(never()).issueUserTokens(any(), any());
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    @Test
    void 활성_사용자에게_TokenPair를_발급하고_Refresh_Token_ID를_저장한다() {
        // given
        User activeUser = createActiveUser();
        TokenPair tokenPair = createTokenPair();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(activeUser));
        given(tokenProvider.issueUserTokens(USER_ID, OAuthClientType.APP))
                .willReturn(tokenPair);

        // when
        AuthenticationInfo result = loginTokenService.issueForLogin(
                USER_ID,
                OAuthClientType.APP
        );

        // then
        assertThat(result).isEqualTo(AuthenticationInfo.authenticated(
                OAuthClientType.APP,
                tokenPair
        ));
        then(refreshTokenStore).should().save(
                USER_ID,
                NEW_REFRESH_TOKEN_ID,
                REFRESH_TOKEN_TTL
        );
        then(tokenProvider).should(never()).issueSignupToken(any(), any());
    }

    @Test
    void Refresh_Token_재발급_시_기존_ID를_소비하고_새_TokenPair로_회전한다() {
        // given
        String refreshToken = "old-refresh-token";
        RefreshTokenClaims claims = new RefreshTokenClaims(
                USER_ID,
                OLD_REFRESH_TOKEN_ID,
                OAuthClientType.APP
        );
        TokenPair newTokenPair = createTokenPair();

        given(tokenProvider.parseRefreshToken(refreshToken)).willReturn(claims);
        given(refreshTokenStore.consume(USER_ID, OLD_REFRESH_TOKEN_ID)).willReturn(true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(createActiveUser()));
        given(tokenProvider.issueUserTokens(USER_ID, OAuthClientType.APP))
                .willReturn(newTokenPair);

        // when
        AuthenticationInfo result = loginTokenService.refresh(
                refreshToken,
                OAuthClientType.APP
        );

        // then
        assertThat(result).isEqualTo(AuthenticationInfo.authenticated(
                OAuthClientType.APP,
                newTokenPair
        ));

        InOrder tokenRotation = inOrder(tokenProvider, refreshTokenStore);
        tokenRotation.verify(tokenProvider).parseRefreshToken(refreshToken);
        tokenRotation.verify(refreshTokenStore).consume(USER_ID, OLD_REFRESH_TOKEN_ID);
        tokenRotation.verify(tokenProvider).issueUserTokens(USER_ID, OAuthClientType.APP);
        tokenRotation.verify(refreshTokenStore).save(
                USER_ID,
                NEW_REFRESH_TOKEN_ID,
                REFRESH_TOKEN_TTL
        );
    }

    @Test
    void 이미_소비한_Refresh_Token은_다시_사용할_수_없다() {
        // given
        String refreshToken = "used-refresh-token";
        RefreshTokenClaims claims = new RefreshTokenClaims(
                USER_ID,
                OLD_REFRESH_TOKEN_ID,
                OAuthClientType.APP
        );

        given(tokenProvider.parseRefreshToken(refreshToken)).willReturn(claims);
        given(refreshTokenStore.consume(USER_ID, OLD_REFRESH_TOKEN_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> loginTokenService.refresh(
                refreshToken,
                OAuthClientType.APP
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
        then(refreshTokenStore).should().consume(USER_ID, OLD_REFRESH_TOKEN_ID);
        then(userRepository).shouldHaveNoInteractions();
        then(tokenProvider).should(never()).issueUserTokens(any(), any());
        then(refreshTokenStore).should(never()).save(any(), any(), any());
    }

    @Test
    void Refresh_Token의_clientType과_요청_경로가_다르면_토큰을_소비하지_않는다() {
        // given
        String refreshToken = "app-refresh-token";
        RefreshTokenClaims claims = new RefreshTokenClaims(
                USER_ID,
                OLD_REFRESH_TOKEN_ID,
                OAuthClientType.APP
        );
        given(tokenProvider.parseRefreshToken(refreshToken)).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> loginTokenService.refresh(
                refreshToken,
                OAuthClientType.WEB
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
        then(refreshTokenStore).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
        then(tokenProvider).should().parseRefreshToken(refreshToken);
        then(tokenProvider).shouldHaveNoMoreInteractions();
    }

    @Test
    void 로그아웃하면_Refresh_Token_ID를_소비한다() {
        // given
        String refreshToken = "refresh-token";
        RefreshTokenClaims claims = new RefreshTokenClaims(
                USER_ID,
                OLD_REFRESH_TOKEN_ID,
                OAuthClientType.WEB
        );
        given(tokenProvider.parseRefreshToken(refreshToken)).willReturn(claims);
        given(refreshTokenStore.consume(USER_ID, OLD_REFRESH_TOKEN_ID)).willReturn(true);

        // when
        loginTokenService.logout(refreshToken, OAuthClientType.WEB);

        // then
        then(tokenProvider).should().parseRefreshToken(refreshToken);
        then(refreshTokenStore).should().consume(USER_ID, OLD_REFRESH_TOKEN_ID);
        then(userRepository).shouldHaveNoInteractions();
        then(tokenProvider).shouldHaveNoMoreInteractions();
    }

    @Test
    void 이미_소비한_Refresh_Token으로_로그아웃해도_성공한다() {
        // given
        String refreshToken = "used-refresh-token";
        RefreshTokenClaims claims = new RefreshTokenClaims(
                USER_ID,
                OLD_REFRESH_TOKEN_ID,
                OAuthClientType.APP
        );
        given(tokenProvider.parseRefreshToken(refreshToken)).willReturn(claims);
        given(refreshTokenStore.consume(USER_ID, OLD_REFRESH_TOKEN_ID)).willReturn(false);

        // when
        loginTokenService.logout(refreshToken, OAuthClientType.APP);

        // then
        then(refreshTokenStore).should().consume(USER_ID, OLD_REFRESH_TOKEN_ID);
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    void Refresh_Token의_clientType과_로그아웃_경로가_다르면_토큰을_소비하지_않는다() {
        // given
        String refreshToken = "app-refresh-token";
        RefreshTokenClaims claims = new RefreshTokenClaims(
                USER_ID,
                OLD_REFRESH_TOKEN_ID,
                OAuthClientType.APP
        );
        given(tokenProvider.parseRefreshToken(refreshToken)).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> loginTokenService.logout(
                refreshToken,
                OAuthClientType.WEB
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
        then(refreshTokenStore).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
    }

    private User createActiveUser() {
        User user = User.create("참참이");
        user.completeTermsAgreement();
        return user;
    }

    private TokenPair createTokenPair() {
        return new TokenPair(
                "access-token",
                "new-refresh-token",
                NEW_REFRESH_TOKEN_ID,
                REFRESH_TOKEN_TTL
        );
    }
}
