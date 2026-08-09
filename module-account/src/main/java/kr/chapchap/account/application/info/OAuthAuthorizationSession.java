package kr.chapchap.account.application.info;

public record OAuthAuthorizationSession(
        OAuthClientType clientType,
        String codeChallenge
) {
}
