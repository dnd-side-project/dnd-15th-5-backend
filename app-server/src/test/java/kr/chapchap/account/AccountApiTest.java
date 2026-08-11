package kr.chapchap.account;

import kr.chapchap.account.api.controller.AccountController;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.command.AccountUpdateCommand;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.service.AccountCommandService;
import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static kr.chapchap.account.api.response.AuthenticationResponseHandler.REFRESH_TOKEN_COOKIE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        CorsConfig.class,
        WebMvcConfig.class,
        GlobalExceptionHandler.class,
        AuthenticationResponseHandler.class
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
    private AccountCommandService accountCommandService;

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

    @Test
    void 닉네임과_프로필_이미지를_함께_수정한다() throws Exception {
        // given
        byte[] profileImageContent = new byte[]{1, 2, 3};
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                profileImageContent
        );
        given(accountCommandService.updateAccount(any(AccountUpdateCommand.class)))
                .willReturn(new AccountInfo(USER_ID, "새찹찹이", PROFILE_IMAGE_URL));

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/accounts/me")
                        .file(profileImage)
                        .param("nickname", "새찹찹이")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.nickname").value("새찹찹이"))
                .andExpect(jsonPath("$.data.profileImageUrl").value(PROFILE_IMAGE_URL));

        ArgumentCaptor<AccountUpdateCommand> commandCaptor =
                ArgumentCaptor.forClass(AccountUpdateCommand.class);
        then(accountCommandService).should().updateAccount(commandCaptor.capture());
        AccountUpdateCommand command = commandCaptor.getValue();
        assertThat(command.userId()).isEqualTo(USER_ID);
        assertThat(command.nickname()).isEqualTo("새찹찹이");
        assertThat(command.profileImageContent()).isEqualTo(profileImageContent);
        assertThat(command.deleteProfileImage()).isFalse();
    }

    @Test
    void 프로필_이미지_삭제를_요청한다() throws Exception {
        // given
        given(accountCommandService.updateAccount(any(AccountUpdateCommand.class)))
                .willReturn(new AccountInfo(USER_ID, NICKNAME, null));

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/accounts/me")
                        .param("deleteProfileImage", "true")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());

        ArgumentCaptor<AccountUpdateCommand> commandCaptor =
                ArgumentCaptor.forClass(AccountUpdateCommand.class);
        then(accountCommandService).should().updateAccount(commandCaptor.capture());
        assertThat(commandCaptor.getValue().deleteProfileImage()).isTrue();
        assertThat(commandCaptor.getValue().profileImageContent()).isNull();
    }

    @Test
    void 빈_닉네임으로_수정하면_입력_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/accounts/me")
                        .param("nickname", " ")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));

        then(accountCommandService).shouldHaveNoInteractions();
    }

    @Test
    void 수정할_값이_없으면_계정_도메인_오류를_반환한다() throws Exception {
        // given
        given(accountCommandService.updateAccount(any(AccountUpdateCommand.class)))
                .willThrow(new BusinessException(AccountErrorCode.ACCOUNT_UPDATE_VALUE_REQUIRED));

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A004"))
                .andExpect(jsonPath("$.message").value("수정할 값을 하나 이상 입력해야 합니다."));
    }

    @Test
    void user_scope로_내_계정을_탈퇴한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, ""))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().secure(REFRESH_TOKEN_COOKIE_NAME, true))
                .andExpect(cookie().path(REFRESH_TOKEN_COOKIE_NAME, "/"))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ));

        then(accountCommandService).should().withdrawAccount(USER_ID);
    }

    @Test
    void Access_Token이_없으면_회원_탈퇴를_요청할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(delete("/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(accountCommandService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_회원_탈퇴를_요청할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(delete("/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(accountCommandService).shouldHaveNoInteractions();
    }
}
