package kr.chapchap.account;

import kr.chapchap.account.application.port.GoogleAuthenticationPort;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class GoogleOAuthFlowIntegrationTest {

    private static final String CODE_CHALLENGE =
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String CODE_VERIFIER =
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GoogleAuthenticationPort googleAuthenticationPort;

    @Autowired
    GoogleOAuthFlowIntegrationTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        given(googleAuthenticationPort.createAuthorizationUri(anyString()))
                .willAnswer(invocation -> java.net.URI.create(
                        "https://accounts.google.com/o/oauth2/v2/auth?state="
                                + invocation.getArgument(0, String.class)
                ));
    }

    @AfterEach
    void 데이터베이스를_정리한다() {
        jdbcTemplate.update("DELETE FROM social_accounts");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void Google_로그인_시작부터_ChapChap_로그인_코드_교환까지_처리한다() throws Exception {
        // given
        MvcResult startResult = mockMvc.perform(get("/oauth/google/start")
                        .param("client", "APP")
                        .param("codeChallenge", CODE_CHALLENGE))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(
                        "https://accounts.google.com/o/oauth2/v2/auth"
                )))
                .andReturn();
        String state = extractQueryParameter(
                startResult.getResponse().getRedirectedUrl(),
                "state"
        );
        given(googleAuthenticationPort.authenticate("authorization-code", state))
                .willReturn("google-sub");

        // when
        MvcResult callbackResult = mockMvc.perform(get("/oauth/google/callback")
                        .param("code", "authorization-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andReturn();
        String loginCode = extractQueryParameter(
                callbackResult.getResponse().getRedirectedUrl(),
                "loginCode"
        );

        // then
        mockMvc.perform(post("/auth/social/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "%s",
                                  "codeVerifier": "%s"
                                }
                                """.formatted(loginCode, CODE_VERIFIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(true))
                .andExpect(jsonPath("$.data.signupToken").isNotEmpty());

        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM social_accounts
                        WHERE provider = 'GOOGLE' AND provider_user_id = 'google-sub'
                        """,
                Long.class
        )).isEqualTo(1L);
        then(googleAuthenticationPort).should()
                .authenticate("authorization-code", state);
    }

    @Test
    void Google_WEB_LOCAL_로그인은_운영_백엔드_콜백_처리_후_로컬_프론트로_이동한다() throws Exception {
        // given
        MvcResult startResult = mockMvc.perform(get("/oauth/google/start")
                        .param("client", "WEB_LOCAL")
                        .param("codeChallenge", CODE_CHALLENGE))
                .andExpect(status().isFound())
                .andReturn();
        String state = extractQueryParameter(
                startResult.getResponse().getRedirectedUrl(),
                "state"
        );
        given(googleAuthenticationPort.authenticate("authorization-code", state))
                .willReturn("google-local-web-sub");

        // when & then
        MvcResult callbackResult = mockMvc.perform(get("/oauth/google/callback")
                        .param("code", "authorization-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "http://localhost:5173/auth/callback?loginCode="
                        )
                ))
                .andReturn();
        String loginCode = extractQueryParameter(
                callbackResult.getResponse().getRedirectedUrl(),
                "loginCode"
        );

        mockMvc.perform(post("/auth/social/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "%s",
                                  "codeVerifier": "%s"
                                }
                                """.formatted(loginCode, CODE_VERIFIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(true));

        then(googleAuthenticationPort).should()
                .authenticate("authorization-code", state);
    }

    private String extractQueryParameter(String uri, String name) {
        return UriComponentsBuilder.fromUriString(uri)
                .build()
                .getQueryParams()
                .getFirst(name);
    }
}
