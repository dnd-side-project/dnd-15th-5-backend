package kr.chapchap.account.application.info;

public record OAuthLoginSession(
        Long userId,
        OAuthClientType clientType
) {
}
