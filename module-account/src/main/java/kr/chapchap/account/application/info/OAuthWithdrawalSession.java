package kr.chapchap.account.application.info;

public record OAuthWithdrawalSession(
        Long userId,
        OAuthClientType clientType
) {

    public static final String STATE_PREFIX = "withdrawal-";
}
