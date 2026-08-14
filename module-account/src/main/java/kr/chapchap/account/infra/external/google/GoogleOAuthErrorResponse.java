package kr.chapchap.account.infra.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record GoogleOAuthErrorResponse(String error) {

    private static final String INVALID_AUTHORIZATION_CODE = "invalid_grant";

    boolean isInvalidAuthorizationCode() {
        return INVALID_AUTHORIZATION_CODE.equals(error);
    }
}
