package kr.chapchap.account;

import jakarta.servlet.http.Cookie;
import kr.chapchap.account.api.controller.AuthenticationController;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.command.TermsAgreementCommand;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.service.KakaoOAuthFlowService;
import kr.chapchap.account.application.service.LoginTokenService;
import kr.chapchap.account.application.service.TermsAgreementService;
import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static kr.chapchap.account.api.response.AuthenticationResponseHandler.REFRESH_TOKEN_COOKIE_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        CorsConfig.class,
        GlobalExceptionHandler.class,
        AuthenticationResponseHandler.class
})
@WebMvcTest(AuthenticationController.class)
class AuthenticationApiTest {

    private static final String CODE_VERIFIER =
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    private final MockMvc mockMvc;

    @MockitoBean
    private KakaoOAuthFlowService kakaoOAuthFlowService;

    @MockitoBean
    private TermsAgreementService termsAgreementService;

    @MockitoBean
    private LoginTokenService loginTokenService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    AuthenticationApiTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void 약관_동의_대기_사용자가_loginCode를_교환하면_Signup_Token만_반환한다() throws Exception {
        // given
        given(kakaoOAuthFlowService.exchange("login-code", CODE_VERIFIER))
                .willReturn(AuthenticationInfo.termsRequired(
                        OAuthClientType.WEB,
                        "signup-token"
                ));

        // when & then
        mockMvc.perform(post("/auth/social/kakao/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "login-code",
                                  "codeVerifier": "%s"
                                }
                                """.formatted(CODE_VERIFIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(true))
                .andExpect(jsonPath("$.data.signupToken").value("signup-token"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        then(kakaoOAuthFlowService).should().exchange("login-code", CODE_VERIFIER);
    }

    @Test
    void WEB_사용자가_loginCode를_교환하면_Access_Token은_JSON으로_Refresh_Token은_쿠키로_반환한다() throws Exception {
        // given
        given(kakaoOAuthFlowService.exchange("login-code", CODE_VERIFIER))
                .willReturn(createAuthenticationInfo(OAuthClientType.WEB));

        // when & then
        mockMvc.perform(post("/auth/social/kakao/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "login-code",
                                  "codeVerifier": "%s"
                                }
                                """.formatted(CODE_VERIFIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(false))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, "refresh-token"))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().secure(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, "/"))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 1_209_600))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ));

        then(kakaoOAuthFlowService).should().exchange("login-code", CODE_VERIFIER);
    }

    @Test
    void APP_사용자가_loginCode를_교환하면_Access_Token과_Refresh_Token을_JSON으로_반환한다() throws Exception {
        // given
        given(kakaoOAuthFlowService.exchange("login-code", CODE_VERIFIER))
                .willReturn(createAuthenticationInfo(OAuthClientType.APP));

        // when & then
        mockMvc.perform(post("/auth/social/kakao/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "login-code",
                                  "codeVerifier": "%s"
                                }
                                """.formatted(CODE_VERIFIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(false))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        then(kakaoOAuthFlowService).should().exchange("login-code", CODE_VERIFIER);
    }

    @Test
    void codeVerifier_형식이_올바르지_않으면_검증_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/social/kakao/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "login-code",
                                  "codeVerifier": "too-short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.codeVerifier")
                        .value("PKCE 코드 검증값 형식이 올바르지 않습니다."));
    }

    @Test
    void WEB_signup_scope로_필수_약관에_동의하면_Access_Token은_JSON으로_Refresh_Token은_쿠키로_반환한다() throws Exception {
        // given
        TermsAgreementCommand command = new TermsAgreementCommand(
                1L,
                OAuthClientType.WEB,
                true,
                true
        );
        given(termsAgreementService.agree(command))
                .willReturn(createAuthenticationInfo(OAuthClientType.WEB));

        // when & then
        mockMvc.perform(post("/auth/signup/terms")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("client_type", "WEB"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTermsAgreed": true,
                                  "privacyPolicyAgreed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(false))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, "refresh-token"));

        then(termsAgreementService).should().agree(command);
    }

    @Test
    void APP_signup_scope로_필수_약관에_동의하면_Access_Token과_Refresh_Token을_JSON으로_반환한다() throws Exception {
        // given
        TermsAgreementCommand command = new TermsAgreementCommand(
                1L,
                OAuthClientType.APP,
                true,
                true
        );
        given(termsAgreementService.agree(command))
                .willReturn(createAuthenticationInfo(OAuthClientType.APP));

        // when & then
        mockMvc.perform(post("/auth/signup/terms")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("client_type", "APP"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTermsAgreed": true,
                                  "privacyPolicyAgreed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.requiresTermsAgreement").value(false))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        then(termsAgreementService).should().agree(command);
    }

    @Test
    void user_scope로_약관_동의를_요청하면_접근_거부를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/signup/terms")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTermsAgreed": true,
                                  "privacyPolicyAgreed": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(termsAgreementService).shouldHaveNoInteractions();
    }

    @Test
    void Signup_Token_없이_약관_동의를_요청하면_인증_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/signup/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTermsAgreed": true,
                                  "privacyPolicyAgreed": true
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(termsAgreementService).shouldHaveNoInteractions();
    }

    @Test
    void APP_Refresh_Token으로_재발급하면_Access_Token과_Refresh_Token을_JSON으로_반환한다() throws Exception {
        // given
        given(loginTokenService.refresh("refresh-token", OAuthClientType.APP))
                .willReturn(createRefreshedAuthenticationInfo(OAuthClientType.APP));

        // when & then
        mockMvc.perform(post("/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        then(loginTokenService).should().refresh("refresh-token", OAuthClientType.APP);
    }

    @Test
    void WEB_Refresh_Token_쿠키로_재발급하면_Access_Token은_JSON으로_새_Refresh_Token은_쿠키로_반환한다() throws Exception {
        // given
        given(loginTokenService.refresh("refresh-token", OAuthClientType.WEB))
                .willReturn(createRefreshedAuthenticationInfo(OAuthClientType.WEB));

        // when & then
        mockMvc.perform(post("/auth/token/refresh/web")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, "new-refresh-token"));

        then(loginTokenService).should().refresh("refresh-token", OAuthClientType.WEB);
    }

    @Test
    void APP_로그아웃하면_요청_본문의_Refresh_Token을_폐기한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        then(loginTokenService).should().logout("refresh-token", OAuthClientType.APP);
    }

    @Test
    void WEB_로그아웃하면_쿠키의_Refresh_Token을_폐기하고_쿠키를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout/web")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, ""))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().secure(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, "/"))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ));

        then(loginTokenService).should().logout("refresh-token", OAuthClientType.WEB);
    }

    @Test
    void Refresh_Token_쿠키가_없어도_WEB_로그아웃에_성공한다() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout/web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, ""))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        then(loginTokenService).shouldHaveNoInteractions();
    }

    private AuthenticationInfo createAuthenticationInfo(OAuthClientType clientType) {
        return AuthenticationInfo.authenticated(clientType, new TokenPair(
                "access-token",
                "refresh-token",
                "refresh-token-id",
                Duration.ofDays(14)
        ));
    }

    private AuthenticationInfo createRefreshedAuthenticationInfo(OAuthClientType clientType) {
        return AuthenticationInfo.authenticated(clientType, new TokenPair(
                "new-access-token",
                "new-refresh-token",
                "new-refresh-token-id",
                Duration.ofDays(14)
        ));
    }
}
