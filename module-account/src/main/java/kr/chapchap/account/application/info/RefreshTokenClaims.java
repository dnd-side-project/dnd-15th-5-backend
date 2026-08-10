package kr.chapchap.account.application.info;

public record RefreshTokenClaims(
        Long userId,
        String tokenId,
        OAuthClientType clientType
) {
}
