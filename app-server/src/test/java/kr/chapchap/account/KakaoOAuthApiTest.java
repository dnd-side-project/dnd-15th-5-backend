package kr.chapchap.account;

import kr.chapchap.account.api.controller.KakaoOAuthController;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.KakaoOAuthFlowService;
import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
@WebMvcTest(KakaoOAuthController.class)
class KakaoOAuthApiTest {

    private static final String CODE_CHALLENGE =
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private final MockMvc mockMvc;

    @MockitoBean
    private KakaoOAuthFlowService kakaoOAuthFlowService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    KakaoOAuthApiTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void WEB으로_카카오_로그인을_시작하면_인가_URI로_이동한다() throws Exception {
        // given
        URI authorizationUri = URI.create("https://kauth.kakao.com/oauth/authorize?state=state");
        given(kakaoOAuthFlowService.createAuthorizationUri(
                OAuthClientType.WEB,
                CODE_CHALLENGE
        ))
                .willReturn(authorizationUri);

        // when & then
        mockMvc.perform(get("/oauth/kakao/start")
                        .param("client", "WEB")
                        .param("codeChallenge", CODE_CHALLENGE))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", authorizationUri.toString()));

        then(kakaoOAuthFlowService).should().createAuthorizationUri(
                OAuthClientType.WEB,
                CODE_CHALLENGE
        );
    }

    @Test
    void 카카오_콜백을_처리하면_클라이언트_URI로_이동한다() throws Exception {
        // given
        URI clientUri = URI.create("chapchap://oauth/callback?loginCode=login-code");
        given(kakaoOAuthFlowService.handleCallback("authorization-code", "state"))
                .willReturn(clientUri);

        // when & then
        mockMvc.perform(get("/oauth/kakao/callback")
                        .param("code", "authorization-code")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", clientUri.toString()));

        then(kakaoOAuthFlowService).should()
                .handleCallback("authorization-code", "state");
    }

    @Test
    void 카카오_로그인을_취소해도_클라이언트_URI로_복귀한다() throws Exception {
        // given
        URI clientUri = URI.create("chapchap://oauth/callback?error=oauth_cancelled");
        given(kakaoOAuthFlowService.handleCancelledCallback("state"))
                .willReturn(clientUri);

        // when & then
        mockMvc.perform(get("/oauth/kakao/callback")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", clientUri.toString()));

        then(kakaoOAuthFlowService).should().handleCancelledCallback("state");
    }

    @Test
    void 지원하지_않는_클라이언트로_로그인을_시작하면_입력값_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/oauth/kakao/start")
                        .param("client", "DESKTOP")
                        .param("codeChallenge", CODE_CHALLENGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));

        then(kakaoOAuthFlowService).shouldHaveNoInteractions();
    }
}
