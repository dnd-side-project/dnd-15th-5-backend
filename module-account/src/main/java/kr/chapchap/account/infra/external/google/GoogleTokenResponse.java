package kr.chapchap.account.infra.external.google;

import com.fasterxml.jackson.annotation.JsonProperty;

record GoogleTokenResponse(
        @JsonProperty("id_token") String idToken
) {
}
