package kr.chapchap.account.infra.security;

import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.RefreshTokenClaims;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.port.TokenProvider;
import kr.chapchap.account.infra.config.JwtProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String SCOPE_CLAIM = "scope";
    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String CLIENT_TYPE_CLAIM = "client_type";
    private static final String SIGNUP_SCOPE = "signup";
    private static final String USER_SCOPE = "user";
    private static final String REFRESH_SCOPE = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties properties;
    private final Clock jwtClock;

    @Override
    public String issueSignupToken(Long userId, OAuthClientType clientType) {
        return issueToken(
                requireUserId(userId),
                requireClientType(clientType),
                SIGNUP_SCOPE,
                properties.signupExpiration(),
                null
        );
    }

    @Override
    public TokenPair issueUserTokens(Long userId, OAuthClientType clientType) {
        Long requiredUserId = requireUserId(userId);
        OAuthClientType requiredClientType = requireClientType(clientType);
        String accessToken = issueToken(
                requiredUserId,
                requiredClientType,
                USER_SCOPE,
                properties.accessExpiration(),
                null
        );

        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = issueToken(
                requiredUserId,
                requiredClientType,
                REFRESH_SCOPE,
                properties.refreshExpiration(),
                refreshTokenId
        );
        return new TokenPair(
                accessToken,
                refreshToken,
                refreshTokenId,
                properties.refreshExpiration()
        );
    }

    @Override
    public RefreshTokenClaims parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
        }

        try {
            Jwt jwt = jwtDecoder.decode(refreshToken);
            if (!REFRESH_SCOPE.equals(jwt.getClaimAsString(SCOPE_CLAIM))
                    || !REFRESH_SCOPE.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))
                    || jwt.getSubject() == null
                    || jwt.getId() == null
                    || jwt.getId().isBlank()
                    || jwt.getExpiresAt() == null) {
                throw new BusinessException(ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
            }

            return new RefreshTokenClaims(
                    Long.valueOf(jwt.getSubject()),
                    jwt.getId(),
                    OAuthClientType.fromClaim(jwt.getClaimAsString(CLIENT_TYPE_CLAIM))
            );
        } catch (JwtException | NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_AUTHENTICATION_CREDENTIALS,
                    exception
            );
        }
    }

    private String issueToken(
            Long userId,
            OAuthClientType clientType,
            String scope,
            Duration expiration,
            String tokenId
    ) {
        Instant issuedAt = jwtClock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(expiration))
                .claim(SCOPE_CLAIM, scope)
                .claim(CLIENT_TYPE_CLAIM, clientType.name());

        if (tokenId != null) {
            claims.id(tokenId)
                    .claim(TOKEN_USE_CLAIM, REFRESH_SCOPE);
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims.build())
        ).getTokenValue();
    }

    private Long requireUserId(Long userId) {
        return Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
    }

    private OAuthClientType requireClientType(OAuthClientType clientType) {
        return Objects.requireNonNull(clientType, "OAuth 클라이언트 유형은 필수입니다.");
    }
}
