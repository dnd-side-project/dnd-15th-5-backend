package kr.chapchap.account;

import kr.chapchap.account.api.controller.AccountController;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.domain.entity.ProfileImageCode;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.AccountCommandService;
import kr.chapchap.account.application.service.AccountQueryService;
import kr.chapchap.account.application.service.AccountWithdrawalService;
import kr.chapchap.account.application.service.DeviceTokenCommandService;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
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

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static kr.chapchap.account.api.response.AuthenticationResponseHandler.REFRESH_TOKEN_COOKIE_NAME;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private final MockMvc mockMvc;

    @MockitoBean
    private AccountQueryService accountQueryService;

    @MockitoBean
    private AccountCommandService accountCommandService;

    @MockitoBean
    private AccountWithdrawalService accountWithdrawalService;

    @MockitoBean
    private DeviceTokenCommandService deviceTokenCommandService;

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
                null,
                "BLUE"
        ));

        // when & then
        mockMvc.perform(get("/v1/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.data.profileImageCode").value("BLUE"))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());

        then(accountQueryService).should().getAccount(USER_ID);
    }

    @Test
    void Access_Token이_없으면_인증_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(accountQueryService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_내_정보를_조회하면_접근_거부를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/accounts/me")
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
        mockMvc.perform(get("/v1/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("invalid-user-id"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C006"));

        then(accountQueryService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCOPE_user", "SCOPE_signup"})
    void 사용_중단된_업로드_API는_인증되어도_차단한다(String authority) throws Exception {
        // given
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/v1/accounts/me/profile-upload")
                        .file(profileImage)
                        .param("nickname", "찹찹이")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority(authority))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));
        then(accountCommandService).shouldHaveNoInteractions();
    }

    @Test
    void 인증_없이_사용_중단된_업로드_API를_호출할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/v1/accounts/me/profile-upload"))
                .andExpect(status().isUnauthorized());
        then(accountCommandService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(ProfileImageCode.class)
    void 여섯_기본_이미지를_이름과_함께_저장한다(ProfileImageCode code) throws Exception {
        // given
        given(accountCommandService.updateProfile(USER_ID, "ㅋㅋ", code.name()))
                .willReturn(new AccountInfo(USER_ID, "ㅋㅋ", null, code.name()));

        // when & then
        mockMvc.perform(patch("/v1/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"  ㅋㅋ  \",\"profileImageCode\":\"" + code + "\"}")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("ㅋㅋ"))
                .andExpect(jsonPath("$.data.profileImageCode").value(code.name()))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());
        then(accountCommandService).should().updateProfile(USER_ID, "ㅋㅋ", code.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nickname\":\" \",\"profileImageCode\":\"BLUE\"}",
            "{\"nickname\":\"dhgud스톤\",\"profileImageCode\":\"BLUE\"}",
            "{\"nickname\":\"가나다라마바사아자차카타파하가나다\",\"profileImageCode\":\"BLUE\"}",
            "{\"nickname\":\"찹찹\"}",
            "{\"profileImageCode\":\"BLUE\"}"
    })
    void 잘못된_프로필_요청은_저장하지_않는다(String body) throws Exception {
        // when & then
        mockMvc.perform(patch("/v1/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isBadRequest());
        then(accountCommandService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GREEN", "0"})
    void 잘못된_이미지_코드의_도메인_오류를_반환한다(String code) throws Exception {
        // given
        given(accountCommandService.updateProfile(USER_ID, "찹찹", code))
                .willThrow(new BusinessException(AccountErrorCode.INVALID_PROFILE_IMAGE_CODE));

        // when & then
        mockMvc.perform(patch("/v1/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"찹찹\",\"profileImageCode\":\"" + code + "\"}")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A016"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ㅋ", "가나다라마바사아자차카타파하ㅋㅋ"})
    void 한_자와_16자_이름을_허용한다(String nickname) throws Exception {
        // given
        given(accountCommandService.updateProfile(USER_ID, nickname, "BLUE"))
                .willReturn(new AccountInfo(USER_ID, nickname, null, "BLUE"));

        // when & then
        mockMvc.perform(patch("/v1/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\",\"profileImageCode\":\"BLUE\"}")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value(nickname));
    }

    @Test
    void signup_scope로_프로필을_수정할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(patch("/v1/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"찹찹\",\"profileImageCode\":\"BLUE\"}")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden());
        then(accountCommandService).shouldHaveNoInteractions();
    }

    @Test
    void user_scope로_내_계정을_탈퇴한다() throws Exception {
        // given
        given(accountWithdrawalService.startWithdrawal(USER_ID, OAuthClientType.WEB))
                .willReturn(Optional.empty());

        // when & then
        mockMvc.perform(delete("/v1/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(USER_ID.toString())
                                        .claim("client_type", "WEB"))
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

        then(accountWithdrawalService).should().startWithdrawal(
                USER_ID,
                OAuthClientType.WEB
        );
    }

    @Test
    void Google_회원_탈퇴는_재인증_URI를_반환한다() throws Exception {
        // given
        URI authorizationUri = URI.create(
                "https://accounts.google.com/o/oauth2/v2/auth?state=withdrawal-state"
        );
        given(accountWithdrawalService.startWithdrawal(USER_ID, OAuthClientType.APP))
                .willReturn(Optional.of(authorizationUri));

        // when & then
        mockMvc.perform(delete("/v1/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(USER_ID.toString())
                                        .claim("client_type", "APP"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        authorizationUri.toString()
                ))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.code").value("S001"));

        then(accountWithdrawalService).should().startWithdrawal(
                USER_ID,
                OAuthClientType.APP
        );
    }

    @Test
    void Access_Token이_없으면_회원_탈퇴를_요청할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(delete("/v1/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(accountCommandService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_회원_탈퇴를_요청할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(delete("/v1/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(accountCommandService).shouldHaveNoInteractions();
    }
}
