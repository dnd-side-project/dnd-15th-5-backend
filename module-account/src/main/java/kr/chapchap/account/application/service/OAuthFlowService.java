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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class OAuthFlowService {

    private static final String OAUTH_CANCELLED = "oauth_cancelled";
    private static final String OAUTH_FAILED = "oauth_failed";
    private static final String ACCOUNT_WITHDRAWN = "account_withdrawn";
    private static final Pattern CODE_CHALLENGE_PATTERN = Pattern.compile(
            "[A-Za-z0-9_-]{43}"
    );

    private final KakaoAuthenticationPort kakaoAuthenticationPort;
    private final GoogleAuthenticationPort googleAuthenticationPort;
    private final SocialLoginService socialLoginService;
    private final OAuthSessionStore oauthSessionStore;
    private final OAuthClientRedirectPort oauthClientRedirectPort;
    private final LoginTokenService loginTokenService;

    public URI createAuthorizationUri(
            String provider,
            OAuthClientType clientType,
            String codeChallenge
    ) {
        if (codeChallenge == null || !CODE_CHALLENGE_PATTERN.matcher(codeChallenge).matches()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        SocialProvider socialProvider = parseProvider(provider);
        String state = oauthSessionStore.createState(
                socialProvider,
                clientType,
                codeChallenge
        );
        return switch (socialProvider) {
            case KAKAO -> kakaoAuthenticationPort.createAuthorizationUri(state);
            case GOOGLE -> googleAuthenticationPort.createAuthorizationUri(state);
        };
    }

    public URI handleCallback(
            String provider,
            String authorizationCode,
            String state
    ) {
        SocialProvider socialProvider = parseProvider(provider);
        OAuthAuthorizationSession authorizationSession = consumeAuthorizationSession(
                socialProvider,
                state
        );
        try {
            String providerUserId = switch (socialProvider) {
                case KAKAO -> kakaoAuthenticationPort.authenticate(authorizationCode);
                case GOOGLE -> googleAuthenticationPort.authenticate(authorizationCode, state);
            };
            Long userId = socialLoginService.login(socialProvider, providerUserId);
            String loginCode = oauthSessionStore.createLoginCode(
                    userId,
                    authorizationSession.clientType().toAuthenticationClientType(),
                    authorizationSession.codeChallenge()
            );
            return oauthClientRedirectPort.createLoginRedirect(
                    authorizationSession.clientType(),
                    loginCode
            );
        } catch (BusinessException exception) {
            log.warn(
                    "OAuth 처리 실패: provider={}, code={}",
                    socialProvider,
                    exception.getErrorCode().getCode()
            );
            return oauthClientRedirectPort.createErrorRedirect(
                    authorizationSession.clientType(),
                    resolveOAuthError(exception)
            );
        }
    }

    public URI handleCancelledCallback(String provider, String state) {
        SocialProvider socialProvider = parseProvider(provider);
        OAuthAuthorizationSession authorizationSession = consumeAuthorizationSession(
                socialProvider,
                state
        );
        return oauthClientRedirectPort.createErrorRedirect(
                authorizationSession.clientType(),
                OAUTH_CANCELLED
        );
    }

    public AuthenticationInfo exchange(String loginCode, String codeVerifier) {
        String codeChallenge = createCodeChallenge(codeVerifier);
        OAuthLoginSession loginSession = oauthSessionStore
                .consumeLoginCode(loginCode, codeChallenge)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
        return loginTokenService.issueForLogin(
                loginSession.userId(),
                loginSession.clientType()
        );
    }

    private OAuthAuthorizationSession consumeAuthorizationSession(
            SocialProvider provider,
            String state
    ) {
        OAuthAuthorizationSession session = oauthSessionStore.consumeState(state)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
        if (session.provider() != provider) {
            throw new BusinessException(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
        }
        return session;
    }

    private SocialProvider parseProvider(String provider) {
        return switch (provider) {
            case "kakao" -> SocialProvider.KAKAO;
            case "google" -> SocialProvider.GOOGLE;
            default -> throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE
            );
        };
    }

    private String resolveOAuthError(BusinessException exception) {
        if (exception.getErrorCode() == AccountErrorCode.ACCOUNT_WITHDRAWN) {
            return ACCOUNT_WITHDRAWN;
        }
        return OAUTH_FAILED;
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
