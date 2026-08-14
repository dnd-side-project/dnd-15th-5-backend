package kr.chapchap.account.application.port;

import kr.chapchap.account.application.info.GoogleWithdrawalAuthenticationInfo;

import java.net.URI;

public interface GoogleAuthenticationPort {

    URI createAuthorizationUri(String state);

    URI createReauthenticationUri(String state);

    String authenticate(String authorizationCode, String nonce);

    GoogleWithdrawalAuthenticationInfo authenticateForWithdrawal(
            String authorizationCode,
            String nonce
    );

    void revoke(String accessToken);
}
