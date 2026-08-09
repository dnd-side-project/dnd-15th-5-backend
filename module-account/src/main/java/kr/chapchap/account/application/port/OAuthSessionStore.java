package kr.chapchap.account.application.port;

import kr.chapchap.account.application.info.OAuthAuthorizationSession;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthLoginSession;

import java.util.Optional;

public interface OAuthSessionStore {

    String createState(OAuthClientType clientType, String codeChallenge);

    Optional<OAuthAuthorizationSession> consumeState(String state);

    String createLoginCode(
            Long userId,
            OAuthClientType clientType,
            String codeChallenge
    );

    Optional<OAuthLoginSession> consumeLoginCode(String loginCode, String codeChallenge);
}
