package kr.chapchap.account.application.info;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;

public enum OAuthClientType {
    WEB,
    APP;

    public static OAuthClientType fromClaim(String value) {
        try {
            return OAuthClientType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                    exception
            );
        }
    }
}
