package kr.chapchap.account.application.port;

import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.RefreshTokenClaims;
import kr.chapchap.account.application.info.TokenPair;

public interface TokenProvider {

    String issueSignupToken(Long userId, OAuthClientType clientType);

    TokenPair issueUserTokens(Long userId, OAuthClientType clientType);

    RefreshTokenClaims parseRefreshToken(String refreshToken);
}
