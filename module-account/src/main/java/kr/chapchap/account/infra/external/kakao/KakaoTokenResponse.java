package kr.chapchap.account.infra.external.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoTokenResponse(
        @JsonProperty("access_token")
        String accessToken
) {
}
