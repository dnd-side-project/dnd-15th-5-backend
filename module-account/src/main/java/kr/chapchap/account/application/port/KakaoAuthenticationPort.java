package kr.chapchap.account.application.port;

import java.net.URI;

public interface KakaoAuthenticationPort {

    URI createAuthorizationUri(String state);

    String authenticate(String authorizationCode);
}
