package kr.chapchap.account.infra.external.kakao;

import kr.chapchap.account.infra.config.KakaoOAuthProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoAuthenticationClientTest {

    private static final URI AUTHORIZATION_URI = URI.create("https://kauth.kakao.test/oauth/authorize");
    private static final URI TOKEN_URI = URI.create("https://kauth.kakao.test/oauth/token");
    private static final URI USER_INFO_URI = URI.create("https://kapi.kakao.test/v2/user/me");
    private static final URI UNLINK_URI = URI.create("https://kapi.kakao.test/v1/user/unlink");
    private static final URI REDIRECT_URI = URI.create("https://client.example.com/oauth/kakao");

    private MockRestServiceServer server;
    private KakaoAuthenticationClient kakaoAuthenticationClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthProperties properties = new KakaoOAuthProperties(
                "rest-api-key",
                "client-secret",
                "admin-key",
                REDIRECT_URI,
                AUTHORIZATION_URI,
                TOKEN_URI,
                USER_INFO_URI,
                UNLINK_URI,
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );
        kakaoAuthenticationClient = new KakaoAuthenticationClient(builder.build(), properties);
    }

    @Test
    void 카카오_인가_URI에_redirect_uri와_state를_포함한다() {
        // when
        URI authorizationUri = kakaoAuthenticationClient.createAuthorizationUri("oauth-state");

        // then
        UriComponents components = UriComponentsBuilder.fromUri(authorizationUri).build();
        assertThat(components.getScheme()).isEqualTo("https");
        assertThat(components.getHost()).isEqualTo("kauth.kakao.test");
        assertThat(components.getPath()).isEqualTo("/oauth/authorize");
        assertThat(components.getQueryParams().getFirst("response_type")).isEqualTo("code");
        assertThat(components.getQueryParams().getFirst("client_id")).isEqualTo("rest-api-key");
        assertThat(components.getQueryParams().getFirst("redirect_uri"))
                .isEqualTo(REDIRECT_URI.toString());
        assertThat(components.getQueryParams().getFirst("state")).isEqualTo("oauth-state");
    }

    @Test
    void 인가_코드를_Access_Token으로_교환하고_카카오_사용자_ID를_조회한다() {
        // given
        expectAccessTokenExchange();
        server.expect(requestTo(USER_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess(
                        """
                                {
                                  "id": 123456789,
                                  "kakao_account": {
                                    "profile": {
                                      "profile_image_url": "https://example.com/profile.png"
                                    }
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        String result = kakaoAuthenticationClient.authenticate("authorization-code");

        // then
        assertThat(result).isEqualTo("123456789");
        server.verify();
    }

    @Test
    void kakao_account가_없어도_사용자_ID를_조회한다() {
        // given
        expectAccessTokenExchange();
        server.expect(requestTo(USER_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "id": 123456789
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        String result = kakaoAuthenticationClient.authenticate("authorization-code");

        // then
        assertThat(result).isEqualTo("123456789");
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
                                  "error_description": "authorization code not found",
                                  "error_code": "KOE320"
                                }
                                """));

        // when & then
        assertThatThrownBy(() -> kakaoAuthenticationClient.authenticate("invalid-code"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
                    assertThat(exception.getCause()).isInstanceOf(RestClientResponseException.class);
                });
        server.verify();
    }

    @Test
    void 카카오_클라이언트_설정_오류는_외부_연동_ErrorCode로_변환한다() {
        // given
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_client",
                                  "error_description": "Bad client credentials",
                                  "error_code": "KOE010"
                                }
                                """));

        // when & then
        assertThatThrownBy(() -> kakaoAuthenticationClient.authenticate("authorization-code"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                    assertThat(exception.getCause()).isInstanceOf(RestClientResponseException.class);
                });
        server.verify();
    }

    @Test
    void 어드민_키와_카카오_사용자_ID로_연결을_해제한다() {
        // given
        server.expect(requestTo(UNLINK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK admin-key"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED
                ))
                .andExpect(content().formDataContains(Map.of(
                        "target_id_type", "user_id",
                        "target_id", "123456789"
                )))
                .andRespond(withSuccess(
                        "{\"id\": 123456789}",
                        MediaType.APPLICATION_JSON
                ));

        // when
        kakaoAuthenticationClient.unlink("123456789");

        // then
        server.verify();
    }

    @Test
    void 이미_연결이_해제된_카카오_사용자는_성공으로_처리한다() {
        // given
        server.expect(requestTo(UNLINK_URI))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": -101,
                                  "msg": "NotRegisteredUserException"
                                }
                                """));

        // when
        kakaoAuthenticationClient.unlink("123456789");

        // then
        server.verify();
    }

    @Test
    void 카카오_연결_해제_실패를_외부_연동_ErrorCode로_변환한다() {
        // given
        server.expect(requestTo(UNLINK_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> kakaoAuthenticationClient.unlink("123456789"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                    assertThat(exception.getCause()).isInstanceOf(
                            RestClientResponseException.class
                    );
                });
        server.verify();
    }

    private void expectAccessTokenExchange() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", "rest-api-key",
                        "client_secret", "client-secret",
                        "redirect_uri", REDIRECT_URI.toString(),
                        "code", "authorization-code"
                )))
                .andRespond(withSuccess(
                        """
                                {"access_token": "kakao-access-token", "token_type": "bearer"}
                                """,
                        MediaType.APPLICATION_JSON
                ));
    }
}
