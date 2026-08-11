package kr.chapchap.account.infra.security;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ActiveUserJwtValidatorTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActiveUserJwtValidator validator;

    @Test
    void 활성_사용자의_Access_Token은_허용한다() {
        // given
        User user = createActiveUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        OAuth2TokenValidatorResult result = validator.validate(createJwt("user", "1"));

        // then
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void 탈퇴_사용자의_Access_Token은_거부한다() {
        // given
        User user = createActiveUser();
        user.withdraw(LocalDateTime.now());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        OAuth2TokenValidatorResult result = validator.validate(createJwt("user", "1"));

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void 사용자_ID가_유효하지_않은_Access_Token은_거부한다() {
        // when
        OAuth2TokenValidatorResult result = validator.validate(
                createJwt("user", "invalid-user-id")
        );

        // then
        assertThat(result.hasErrors()).isTrue();
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_사용자의_Access_Token은_거부한다() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when
        OAuth2TokenValidatorResult result = validator.validate(createJwt("user", "1"));

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void Access_Token이_아닌_JWT는_사용자_상태를_확인하지_않는다() {
        // when
        OAuth2TokenValidatorResult signupResult = validator.validate(createJwt("signup", "1"));
        OAuth2TokenValidatorResult refreshResult = validator.validate(createJwt("refresh", "1"));

        // then
        assertThat(signupResult.hasErrors()).isFalse();
        assertThat(refreshResult.hasErrors()).isFalse();
        then(userRepository).shouldHaveNoInteractions();
    }

    private Jwt createJwt(String scope, String subject) {
        Instant issuedAt = Instant.parse("2026-08-11T00:00:00Z");
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(1800))
                .claim("scope", scope)
                .build();
    }

    private User createActiveUser() {
        User user = User.create("찹찹이");
        user.completeTermsAgreement();
        return user;
    }
}
