package kr.chapchap.account.application.port;

import java.net.URI;

public interface GoogleAuthenticationPort {

    URI createAuthorizationUri(String state);

    String authenticate(String authorizationCode, String nonce);
}
