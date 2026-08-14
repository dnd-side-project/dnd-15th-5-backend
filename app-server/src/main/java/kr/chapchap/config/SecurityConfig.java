package kr.chapchap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.core.exception.ErrorCode;
import kr.chapchap.core.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(withDefaults())
                        .authenticationEntryPoint((request, response, exception) ->
                                handleSecurityError(
                                        request,
                                        response,
                                        objectMapper,
                                        CommonErrorCode.AUTHENTICATION_REQUIRED
                                )
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                handleSecurityError(
                                        request,
                                        response,
                                        objectMapper,
                                        CommonErrorCode.ACCESS_DENIED
                                )
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/oauth/kakao/start", "/oauth/kakao/callback")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/social/kakao/exchange",
                                "/auth/token/refresh",
                                "/auth/token/refresh/web",
                                "/auth/logout",
                                "/auth/logout/web"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup/terms")
                        .hasAuthority("SCOPE_signup")
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/accounts/test",
                                "/consumptions",
                                "/consumptions/**",
                                "/reports",
                                "/reports/**",
                                "/v3/api-docs/**",
                                "/swagger-ui",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().hasAuthority("SCOPE_user")
                );

        return http.build();
    }

    private void handleSecurityError(
            HttpServletRequest request,
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ErrorCode errorCode
    ) throws IOException {
        log.warn(
                "[{}] {} method={}, uri={}",
                errorCode.getCode(),
                errorCode.getMessage(),
                request.getMethod(),
                request.getRequestURI()
        );
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}
