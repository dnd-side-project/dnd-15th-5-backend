package kr.chapchap.account.application.port;

import kr.chapchap.account.application.info.OAuthAuthorizationSession;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthLoginSession;
import kr.chapchap.account.domain.entity.SocialProvider;

import java.util.Optional;

public interface OAuthSessionStore {

    String createState(
            SocialProvider provider,
            OAuthClientType clientType,
            String codeChallenge
    );

    Optional<OAuthAuthorizationSession> consumeState(String state);

    String createLoginCode(
            Long userId,
            OAuthClientType clientType,
            String codeChallenge
    );

    Optional<OAuthLoginSession> consumeLoginCode(String loginCode, String codeChallenge);
}
