package kr.chapchap.account.domain.entity;

import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;

public enum ProfileImageCode {
    BLUE,
    SKY_BLUE,
    PINK,
    YELLOW,
    TEAL,
    RED;

    public static ProfileImageCode from(String value) {
        if (value == null) {
            throw new BusinessException(AccountErrorCode.INVALID_PROFILE_IMAGE_CODE);
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(AccountErrorCode.INVALID_PROFILE_IMAGE_CODE);
        }
    }
}
