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
public class KakaoOAuthFlowService {

    private static final String OAUTH_CANCELLED = "oauth_cancelled";
    private static final String OAUTH_FAILED = "oauth_failed";
    private static final Pattern CODE_CHALLENGE_PATTERN = Pattern.compile(
            "[A-Za-z0-9_-]{43}"
    );

    private final KakaoAuthenticationPort kakaoAuthenticationPort;
    private final SocialLoginService socialLoginService;
    private final OAuthSessionStore oauthSessionStore;
    private final OAuthClientRedirectPort oauthClientRedirectPort;
    private final LoginTokenService loginTokenService;

    public URI createAuthorizationUri(OAuthClientType clientType, String codeChallenge) {
        if (codeChallenge == null || !CODE_CHALLENGE_PATTERN.matcher(codeChallenge).matches()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        String state = oauthSessionStore.createState(clientType, codeChallenge);
        return kakaoAuthenticationPort.createAuthorizationUri(state);
    }

    public URI handleCallback(String authorizationCode, String state) {
        OAuthAuthorizationSession authorizationSession = consumeAuthorizationSession(state);
        try {
            String providerUserId = kakaoAuthenticationPort.authenticate(authorizationCode);
            Long userId = socialLoginService.login(
                    SocialProvider.KAKAO,
                    providerUserId
            );
            String loginCode = oauthSessionStore.createLoginCode(
                    userId,
                    authorizationSession.clientType(),
                    authorizationSession.codeChallenge()
            );
            return oauthClientRedirectPort.createLoginRedirect(
                    authorizationSession.clientType(),
                    loginCode
            );
        } catch (BusinessException exception) {
            log.warn("카카오 OAuth 처리 실패: code={}", exception.getErrorCode().getCode());
            return oauthClientRedirectPort.createErrorRedirect(
                    authorizationSession.clientType(),
                    OAUTH_FAILED
            );
        }
    }

    public URI handleCancelledCallback(String state) {
        OAuthAuthorizationSession authorizationSession = consumeAuthorizationSession(state);
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

    private OAuthAuthorizationSession consumeAuthorizationSession(String state) {
        return oauthSessionStore.consumeState(state)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
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
