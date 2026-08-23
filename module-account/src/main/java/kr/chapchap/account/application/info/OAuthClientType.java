package kr.chapchap.account.application.info;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;

public enum OAuthClientType {
    WEB,
    WEB_LOCAL,
    APP;

    public OAuthClientType toAuthenticationClientType() {
        return this == WEB_LOCAL ? WEB : this;
    }

    public static OAuthClientType fromClaim(String value) {
        try {
            return OAuthClientType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(
                    CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                    exception
            );
        }
    }
}
