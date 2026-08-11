package kr.chapchap.account;

import kr.chapchap.account.api.controller.AccountController;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        CorsConfig.class,
        WebMvcConfig.class,
        GlobalExceptionHandler.class
})
@WebMvcTest(AccountController.class)
class AccountApiTest {

    private static final Long USER_ID = 1L;
    private static final String NICKNAME = "찹찹이";
    private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

    private final MockMvc mockMvc;

    @MockitoBean
    private AccountQueryService accountQueryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    AccountApiTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void user_scope로_내_정보를_조회한다() throws Exception {
        // given
        given(accountQueryService.getAccount(USER_ID)).willReturn(new AccountInfo(
                USER_ID,
                NICKNAME,
                PROFILE_IMAGE_URL
        ));

        // when & then
        mockMvc.perform(get("/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.data.profileImageUrl").value(PROFILE_IMAGE_URL));

        then(accountQueryService).should().getAccount(USER_ID);
    }

    @Test
    void Access_Token이_없으면_인증_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(accountQueryService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_내_정보를_조회하면_접근_거부를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(accountQueryService).shouldHaveNoInteractions();
    }

    @Test
    void 숫자가_아닌_JWT_subject로_조회하면_인증_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("invalid-user-id"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C006"));

        then(accountQueryService).shouldHaveNoInteractions();
    }
}
