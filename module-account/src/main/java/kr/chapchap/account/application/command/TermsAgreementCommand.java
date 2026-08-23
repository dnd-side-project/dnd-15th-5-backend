package kr.chapchap.account.application.command;

import kr.chapchap.account.application.info.OAuthClientType;

public record TermsAgreementCommand(
        Long userId,
        OAuthClientType clientType,
        boolean serviceTermsAgreed
) {
}
