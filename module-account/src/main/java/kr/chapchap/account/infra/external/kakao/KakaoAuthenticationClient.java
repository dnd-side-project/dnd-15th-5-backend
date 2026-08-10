package kr.chapchap.account.infra.external.kakao;

import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.infra.config.KakaoOAuthProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RequiredArgsConstructor
@Component
public class KakaoAuthenticationClient implements KakaoAuthenticationPort {

    private static final String AUTHORIZATION_CODE_GRANT = "authorization_code";

    private final RestClient kakaoRestClient;
    private final KakaoOAuthProperties properties;

    @Override
    public URI createAuthorizationUri(String state) {
        if (!StringUtils.hasText(state)) {
            throw new IllegalArgumentException("OAuth state는 비어 있을 수 없습니다.");
        }

        return UriComponentsBuilder.fromUri(properties.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }

    @Override
    public String authenticate(String authorizationCode) {
        String accessToken = requestAccessToken(authorizationCode);
        return requestProviderUserId(accessToken);
    }

    private String requestAccessToken(String authorizationCode) {
        try {
            return exchangeAuthorizationCode(authorizationCode);
        } catch (RestClientResponseException exception) {
            if (isInvalidAuthorizationCode(exception)) {
                throw new BusinessException(
                        ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                        exception
                );
            }

            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private String exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", AUTHORIZATION_CODE_GRANT);
        formData.add("client_id", properties.clientId());
        formData.add("redirect_uri", properties.redirectUri().toString());
        formData.add("code", authorizationCode);
        formData.add("client_secret", properties.clientSecret());

        KakaoTokenResponse response = kakaoRestClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        return response.accessToken();
    }

    private String requestProviderUserId(String accessToken) {
        try {
            KakaoUserResponse response = kakaoRestClient.get()
                    .uri(properties.userInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }

            return response.id().toString();
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private boolean isInvalidAuthorizationCode(RestClientResponseException exception) {
        try {
            KakaoOAuthErrorResponse response = exception.getResponseBodyAs(
                    KakaoOAuthErrorResponse.class
            );
            return response != null && response.isInvalidAuthorizationCode();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
