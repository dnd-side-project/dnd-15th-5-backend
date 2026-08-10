package kr.chapchap.account.application.info;

import java.time.Duration;

public record AuthenticationInfo(
        OAuthClientType clientType,
        boolean requiresTermsAgreement,
        String signupToken,
        String accessToken,
        String refreshToken,
        Duration refreshTokenExpiresIn
) {

    public static AuthenticationInfo termsRequired(
            OAuthClientType clientType,
            String signupToken
    ) {
        return new AuthenticationInfo(
                clientType,
                true,
                signupToken,
                null,
                null,
                null
        );
    }

    public static AuthenticationInfo authenticated(
            OAuthClientType clientType,
            TokenPair tokenPair
    ) {
        return new AuthenticationInfo(
                clientType,
                false,
                null,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresIn()
        );
    }
}
