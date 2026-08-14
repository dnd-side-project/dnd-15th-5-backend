package kr.chapchap.account.application.port;

import kr.chapchap.account.application.info.OAuthClientType;

import java.net.URI;

public interface OAuthClientRedirectPort {

    URI createLoginRedirect(OAuthClientType clientType, String loginCode);

    URI createWithdrawalRedirect(OAuthClientType clientType);

    URI createErrorRedirect(OAuthClientType clientType, String errorCode);
}
