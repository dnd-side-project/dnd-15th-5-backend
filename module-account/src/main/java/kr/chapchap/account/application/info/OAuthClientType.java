package kr.chapchap.account.application.info;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;

public enum OAuthClientType {
    WEB,
    APP;

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
