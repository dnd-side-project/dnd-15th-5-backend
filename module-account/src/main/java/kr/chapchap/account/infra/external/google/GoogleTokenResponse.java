package kr.chapchap.account.infra.external.google;

import com.fasterxml.jackson.annotation.JsonProperty;

record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken
) {
}
