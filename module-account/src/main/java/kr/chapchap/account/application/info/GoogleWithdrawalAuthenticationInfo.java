package kr.chapchap.account.application.info;

public record GoogleWithdrawalAuthenticationInfo(
        String providerUserId,
        String accessToken
) {
}
