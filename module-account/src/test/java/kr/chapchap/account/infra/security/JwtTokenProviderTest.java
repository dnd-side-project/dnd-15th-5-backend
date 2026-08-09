package kr.chapchap.account.infra.security;

import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.RefreshTokenClaims;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.infra.config.JwtConfig;
import kr.chapchap.account.infra.config.JwtProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "01234567890123456789012345678901";
    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    private JwtProperties properties;
    private Clock clock;
    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void 토큰_제공자를_생성한다() {
        properties = new JwtProperties(
                SECRET,
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        );
        clock = Clock.fixed(NOW, ZoneOffset.UTC);

        JwtConfig jwtConfig = new JwtConfig();
        jwtEncoder = jwtConfig.jwtEncoder(properties);
        jwtDecoder = jwtConfig.jwtDecoder(properties, clock);
        tokenProvider = new JwtTokenProvider(
                jwtEncoder,
                jwtDecoder,
                properties,
                clock
        );
    }

    @Test
    void Signup_Token에_사용자와_signup_scope를_담는다() {
        // when
        String signupToken = tokenProvider.issueSignupToken(
                1L,
                OAuthClientType.WEB
        );

        // then
        Jwt jwt = jwtDecoder.decode(signupToken);
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("signup");
        assertThat(jwt.getClaimAsString("client_type")).isEqualTo("WEB");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(properties.signupExpiration()));
    }

    @Test
    void TokenPair에_Access_Token과_Refresh_Token의_용도를_구분해_담는다() {
        // when
        TokenPair tokenPair = tokenProvider.issueUserTokens(
                1L,
                OAuthClientType.APP
        );

        // then
        Jwt accessToken = jwtDecoder.decode(tokenPair.accessToken());
        Jwt refreshToken = jwtDecoder.decode(tokenPair.refreshToken());
        RefreshTokenClaims refreshTokenClaims = tokenProvider.parseRefreshToken(
                tokenPair.refreshToken()
        );

        assertThat(accessToken.getSubject()).isEqualTo("1");
        assertThat(accessToken.getClaimAsString("scope")).isEqualTo("user");
        assertThat(accessToken.getClaimAsString("client_type")).isEqualTo("APP");
        assertThat(accessToken.getExpiresAt()).isEqualTo(NOW.plus(properties.accessExpiration()));
        assertThat(refreshToken.getClaimAsString("scope")).isEqualTo("refresh");
        assertThat(refreshToken.getClaimAsString("token_use")).isEqualTo("refresh");
        assertThat(refreshToken.getClaimAsString("client_type")).isEqualTo("APP");
        assertThat(refreshToken.getId()).isEqualTo(tokenPair.refreshTokenId());
        assertThat(refreshToken.getExpiresAt()).isEqualTo(NOW.plus(properties.refreshExpiration()));
        assertThat(tokenPair.refreshTokenExpiresIn()).isEqualTo(properties.refreshExpiration());
        assertThat(refreshTokenClaims).isEqualTo(new RefreshTokenClaims(
                1L,
                tokenPair.refreshTokenId(),
                OAuthClientType.APP
        ));
    }

    @Test
    void Access_Token은_Refresh_Token으로_해석할_수_없다() {
        // given
        TokenPair tokenPair = tokenProvider.issueUserTokens(
                1L,
                OAuthClientType.APP
        );

        // when & then
        assertThatThrownBy(() -> tokenProvider.parseRefreshToken(
                tokenPair.accessToken()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
    }

    @Test
    void 만료된_Refresh_Token은_해석할_수_없다() {
        // given
        TokenPair tokenPair = tokenProvider.issueUserTokens(
                1L,
                OAuthClientType.APP
        );
        Clock expiredClock = Clock.fixed(
                NOW.plus(properties.refreshExpiration()).plus(Duration.ofMinutes(2)),
                ZoneOffset.UTC
        );
        JwtConfig jwtConfig = new JwtConfig();
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                jwtEncoder,
                jwtConfig.jwtDecoder(properties, expiredClock),
                properties,
                expiredClock
        );

        // when & then
        assertThatThrownBy(() -> expiredTokenProvider.parseRefreshToken(
                tokenPair.refreshToken()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
    }
}
