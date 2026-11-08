package kr.chapchap.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.chapchap.account.api.controller.DevAuthenticationController;
import kr.chapchap.account.api.response.AuthenticationResponseHandler;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.application.port.TokenProvider;
import kr.chapchap.account.application.service.LoginTokenService;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevAuthenticationApiTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(
                    WebConfiguration.class,
                    SecurityConfig.class,
                    DevAuthenticationController.class,
                    AuthenticationResponseHandler.class,
                    LoginTokenService.class,
                    GlobalExceptionHandler.class
            )
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(JwtDecoder.class, () -> mock(JwtDecoder.class))
            .withBean(UserRepository.class, () -> userRepository)
            .withBean(TokenProvider.class, () -> tokenProvider)
            .withBean(RefreshTokenStore.class, () -> refreshTokenStore);

    @ParameterizedTest
    @ValueSource(strings = {"local", "dev", "prod,dev"})
    void 로컬과_개발에서는_인증_없이_활성_사용자의_토큰을_발급한다(String profiles) {
        // given
        User user = User.create("테스트회원");
        user.completeTermsAgreement();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tokenProvider.issueUserTokens(1L, OAuthClientType.APP))
                .willReturn(new TokenPair("access-token", "refresh-token", "token-id", Duration.ofDays(14)));

        contextRunner.withPropertyValues("spring.profiles.active=" + profiles).run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

            // when
            var result = mockMvc.perform(post("/v1/auth/dev/token"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
            then(refreshTokenStore).should().save(1L, "token-id", Duration.ofDays(14));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "prod,monitoring", "default"})
    void 운영과_기본_환경에는_테스트_API가_등록되지_않는다(String profiles) {
        // given
        contextRunner.withPropertyValues("spring.profiles.active=" + profiles).run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

            // when
            var result = mockMvc.perform(post("/v1/auth/dev/token"));

            // then
            assertThat(context).doesNotHaveBean(DevAuthenticationController.class);
            result.andExpect(status().isUnauthorized());
            mockMvc.perform(post("/v1/auth/dev/token").with(jwt()))
                    .andExpect(status().isForbidden());
            then(userRepository).shouldHaveNoInteractions();
            then(tokenProvider).shouldHaveNoInteractions();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "pending", "withdrawn"})
    void 없거나_비활성인_사용자에게는_토큰을_발급하지_않는다(String userState) {
        // given
        User user = User.create("테스트회원");
        if (userState.equals("withdrawn")) {
            user.completeTermsAgreement();
            user.withdraw(LocalDateTime.of(2026, 1, 1, 0, 0));
        }
        given(userRepository.findById(2L))
                .willReturn(userState.equals("missing") ? Optional.empty() : Optional.of(user));

        contextRunner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

            // when
            var result = mockMvc.perform(post("/v1/auth/dev/token").param("userId", "2"));

            // then
            result.andExpect(status().is(userState.equals("missing") ? 401 : 403));
            then(tokenProvider).shouldHaveNoInteractions();
            then(refreshTokenStore).shouldHaveNoInteractions();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    static class WebConfiguration {
    }
}
