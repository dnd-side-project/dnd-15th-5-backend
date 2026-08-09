package kr.chapchap.account.infra.external.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoOAuthErrorResponse(
        @JsonProperty("error_code")
        String errorCode
) {

    private static final String INVALID_AUTHORIZATION_CODE = "KOE320";

    boolean isInvalidAuthorizationCode() {
        return INVALID_AUTHORIZATION_CODE.equals(errorCode);
    }
}
