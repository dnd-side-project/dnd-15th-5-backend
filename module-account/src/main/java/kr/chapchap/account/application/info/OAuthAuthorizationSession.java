package kr.chapchap.account.application.info;

import kr.chapchap.account.domain.entity.SocialProvider;

public record OAuthAuthorizationSession(
        SocialProvider provider,
        OAuthClientType clientType,
        String codeChallenge
) {
}
