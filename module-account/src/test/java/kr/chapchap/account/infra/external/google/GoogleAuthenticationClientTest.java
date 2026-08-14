package kr.chapchap.account.infra.external.google;

import kr.chapchap.account.infra.config.GoogleOAuthProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleAuthenticationClientTest {

    private static final String CLIENT_ID = "google-client-id";
    private static final URI AUTHORIZATION_URI = URI.create(
            "https://accounts.google.test/o/oauth2/v2/auth"
    );
    private static final URI TOKEN_URI = URI.create("https://oauth2.google.test/token");
    private static final URI REDIRECT_URI = URI.create(
            "https://client.example.com/oauth/google/callback"
    );
    private static final String STATE = "oauth-state";

    private MockRestServiceServer server;
    private JwtDecoder idTokenDecoder;
    private GoogleAuthenticationClient googleAuthenticationClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        idTokenDecoder = mock(JwtDecoder.class);
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                CLIENT_ID,
                "google-client-secret",
                REDIRECT_URI,
                AUTHORIZATION_URI,
                TOKEN_URI,
                URI.create("https://www.google.test/oauth2/v3/certs"),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );
        googleAuthenticationClient = new GoogleAuthenticationClient(
                builder.build(),
                properties,
                idTokenDecoder
        );
    }

    @Test
    void Google_인가_URI에_OpenID와_email_scope와_state와_nonce를_포함한다() {
        // when
        URI authorizationUri = googleAuthenticationClient.createAuthorizationUri(STATE);

        // then
        UriComponents components = UriComponentsBuilder.fromUri(authorizationUri).build();
        assertThat(components.getScheme()).isEqualTo("https");
        assertThat(components.getHost()).isEqualTo("accounts.google.test");
        assertThat(components.getPath()).isEqualTo("/o/oauth2/v2/auth");
        assertThat(components.getQueryParams().getFirst("response_type")).isEqualTo("code");
        assertThat(components.getQueryParams().getFirst("client_id")).isEqualTo(CLIENT_ID);
        assertThat(components.getQueryParams().getFirst("redirect_uri"))
                .isEqualTo(REDIRECT_URI.toString());
        assertThat(components.getQueryParams().getFirst("scope")).isEqualTo("openid%20email");
        assertThat(components.getQueryParams().getFirst("state")).isEqualTo(STATE);
        assertThat(components.getQueryParams().getFirst("nonce")).isEqualTo(STATE);
        assertThat(components.getQueryParams()).doesNotContainKeys("access_type", "prompt");
    }

    @Test
    void 인가_코드를_ID_Token으로_교환하고_검증된_sub를_반환한다() {
        // given
        expectIdTokenExchange();
        given(idTokenDecoder.decode("google-id-token"))
                .willReturn(createIdToken(
                        "https://accounts.google.com",
                        List.of(CLIENT_ID),
                        STATE,
                        "google-sub"
                ));

        // when
        String result = googleAuthenticationClient.authenticate(
                "authorization-code",
                STATE
        );

        // then
        assertThat(result).isEqualTo("google-sub");
        server.verify();
    }

    @Test
    void 유효하지_않은_인가_코드는_인증_ErrorCode로_변환한다() {
        // given
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_grant",
                                  "error_description": "Bad Request"
                                }
                                """));

        // when & then
        assertThatThrownBy(() -> googleAuthenticationClient.authenticate(
                "invalid-code",
                STATE
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getErrorCode())
                    .isEqualTo(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
            assertThat(exception.getCause()).isInstanceOf(RestClientResponseException.class);
        });
        server.verify();
    }

    @Test
    void Google_서버_오류는_외부_연동_ErrorCode로_변환한다() {
        // given
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> googleAuthenticationClient.authenticate(
                "authorization-code",
                STATE
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE)
        );
        server.verify();
    }

    @Test
    void ID_Token의_서명이나_만료_검증에_실패하면_인증을_거부한다() {
        // given
        expectIdTokenExchange();
        given(idTokenDecoder.decode("google-id-token"))
                .willThrow(new JwtException("invalid id token"));

        // when & then
        assertInvalidIdToken();
    }

    @Test
    void ID_Token의_발급자가_Google이_아니면_인증을_거부한다() {
        // given
        expectIdTokenExchange();
        given(idTokenDecoder.decode("google-id-token"))
                .willReturn(createIdToken(
                        "https://attacker.example.com",
                        List.of(CLIENT_ID),
                        STATE,
                        "google-sub"
                ));

        // when & then
        assertInvalidIdToken();
    }

    @Test
    void ID_Token의_Audience가_Client_ID와_다르면_인증을_거부한다() {
        // given
        expectIdTokenExchange();
        given(idTokenDecoder.decode("google-id-token"))
                .willReturn(createIdToken(
                        "https://accounts.google.com",
                        List.of("another-client-id"),
                        STATE,
                        "google-sub"
                ));

        // when & then
        assertInvalidIdToken();
    }

    @Test
    void ID_Token의_nonce가_OAuth_state와_다르면_인증을_거부한다() {
        // given
        expectIdTokenExchange();
        given(idTokenDecoder.decode("google-id-token"))
                .willReturn(createIdToken(
                        "https://accounts.google.com",
                        List.of(CLIENT_ID),
                        "another-state",
                        "google-sub"
                ));

        // when & then
        assertInvalidIdToken();
    }

    @Test
    void ID_Token의_sub가_비어_있으면_인증을_거부한다() {
        // given
        expectIdTokenExchange();
        given(idTokenDecoder.decode("google-id-token"))
                .willReturn(createIdToken(
                        "https://accounts.google.com",
                        List.of(CLIENT_ID),
                        STATE,
                        " "
                ));

        // when & then
        assertInvalidIdToken();
    }

    @Test
    void ID_Token에_만료_시간이_없으면_인증을_거부한다() {
        // given
        expectIdTokenExchange();
        Instant now = Instant.now();
        Jwt idTokenWithoutExpiration = Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .claim("iss", "https://accounts.google.com")
                .claim("aud", List.of(CLIENT_ID))
                .claim("nonce", STATE)
                .claim("sub", "google-sub")
                .issuedAt(now)
                .build();
        given(idTokenDecoder.decode("google-id-token"))
                .willReturn(idTokenWithoutExpiration);

        // when & then
        assertInvalidIdToken();
    }

    private void expectIdTokenExchange() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED
                ))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", CLIENT_ID,
                        "client_secret", "google-client-secret",
                        "redirect_uri", REDIRECT_URI.toString(),
                        "code", "authorization-code"
                )))
                .andRespond(withSuccess(
                        """
                                {
                                  "access_token": "google-access-token",
                                  "id_token": "google-id-token",
                                  "token_type": "Bearer"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));
    }

    private Jwt createIdToken(
            String issuer,
            List<String> audience,
            String nonce,
            String subject
    ) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .claim("iss", issuer)
                .claim("aud", audience)
                .claim("nonce", nonce)
                .claim("sub", subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }

    private void assertInvalidIdToken() {
        assertThatThrownBy(() -> googleAuthenticationClient.authenticate(
                "authorization-code",
                STATE
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
        server.verify();
    }
}
