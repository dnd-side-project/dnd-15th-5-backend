package kr.chapchap.account.infra.external.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUserResponse(
        Long id
) {
}
