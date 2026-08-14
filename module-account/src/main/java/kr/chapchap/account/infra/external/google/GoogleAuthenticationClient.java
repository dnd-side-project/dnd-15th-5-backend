package kr.chapchap.account.infra.external.google;

import kr.chapchap.account.application.port.GoogleAuthenticationPort;
import kr.chapchap.account.infra.config.GoogleOAuthProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

public class GoogleAuthenticationClient implements GoogleAuthenticationPort {

    private static final String AUTHORIZATION_CODE_GRANT = "authorization_code";
    private static final String OPEN_ID_SCOPES = "openid";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final RestClient googleRestClient;
    private final GoogleOAuthProperties properties;
    private final JwtDecoder idTokenDecoder;

    public GoogleAuthenticationClient(
            RestClient googleRestClient,
            GoogleOAuthProperties properties,
            JwtDecoder idTokenDecoder
    ) {
        this.googleRestClient = googleRestClient;
        this.properties = properties;
        this.idTokenDecoder = idTokenDecoder;
    }

    @Override
    public URI createAuthorizationUri(String state) {
        if (!StringUtils.hasText(state)) {
            throw new IllegalArgumentException("OAuth state는 비어 있을 수 없습니다.");
        }

        return UriComponentsBuilder.fromUri(properties.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", OPEN_ID_SCOPES)
                .queryParam("state", state)
                .queryParam("nonce", state)
                .build()
                .encode()
                .toUri();
    }

    @Override
    public String authenticate(String authorizationCode, String nonce) {
        if (!StringUtils.hasText(authorizationCode) || !StringUtils.hasText(nonce)) {
            throw new BusinessException(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
        }

        String idToken = requestIdToken(authorizationCode);
        return verifyIdToken(idToken, nonce);
    }

    private String requestIdToken(String authorizationCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", AUTHORIZATION_CODE_GRANT);
        formData.add("client_id", properties.clientId());
        formData.add("client_secret", properties.clientSecret());
        formData.add("redirect_uri", properties.redirectUri().toString());
        formData.add("code", authorizationCode);

        try {
            GoogleTokenResponse response = googleRestClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || !StringUtils.hasText(response.idToken())) {
                throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
            return response.idToken();
        } catch (RestClientResponseException exception) {
            if (isInvalidAuthorizationCode(exception)) {
                throw new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                        exception
                );
            }
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        } catch (RestClientException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private String verifyIdToken(String idToken, String nonce) {
        try {
            Jwt jwt = idTokenDecoder.decode(idToken);
            String issuer = jwt.getClaimAsString("iss");
            String tokenNonce = jwt.getClaimAsString("nonce");
            String subject = jwt.getSubject();
            List<String> audience = jwt.getAudience();
            if (!GOOGLE_ISSUERS.contains(issuer)
                    || audience == null
                    || !audience.contains(properties.clientId())
                    || jwt.getExpiresAt() == null
                    || !nonce.equals(tokenNonce)
                    || !StringUtils.hasText(subject)) {
                throw new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                );
            }
            return subject;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(
                    CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                    exception
            );
        }
    }

    private boolean isInvalidAuthorizationCode(RestClientResponseException exception) {
        try {
            GoogleOAuthErrorResponse response = exception.getResponseBodyAs(
                    GoogleOAuthErrorResponse.class
            );
            return response != null && response.isInvalidAuthorizationCode();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
