package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.RefreshTokenClaims;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.application.port.TokenProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LoginTokenService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public AuthenticationInfo issueForLogin(Long userId, OAuthClientType clientType) {
        User user = findUser(userId);
        if (user.isPendingTerms()) {
            return AuthenticationInfo.termsRequired(
                    clientType,
                    tokenProvider.issueSignupToken(userId, clientType)
            );
        }
        if (!user.isActive()) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        return issueUserTokens(userId, clientType);
    }

    public AuthenticationInfo issueForActiveUser(
            Long userId,
            OAuthClientType clientType
    ) {
        User user = findUser(userId);
        if (!user.isActive()) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        return issueUserTokens(userId, clientType);
    }

    public AuthenticationInfo refresh(
            String refreshToken,
            OAuthClientType expectedClientType
    ) {
        RefreshTokenClaims claims = parseRefreshToken(refreshToken, expectedClientType);
        if (!refreshTokenStore.consume(claims.userId(), claims.tokenId())) {
            throw new BusinessException(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
        }

        User user = findUser(claims.userId());
        if (!user.isActive()) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        return issueUserTokens(claims.userId(), claims.clientType());
    }

    public void logout(
            String refreshToken,
            OAuthClientType expectedClientType
    ) {
        RefreshTokenClaims claims = parseRefreshToken(refreshToken, expectedClientType);
        refreshTokenStore.consume(claims.userId(), claims.tokenId());
    }

    private AuthenticationInfo issueUserTokens(
            Long userId,
            OAuthClientType clientType
    ) {
        TokenPair tokenPair = tokenProvider.issueUserTokens(userId, clientType);
        refreshTokenStore.save(
                userId,
                tokenPair.refreshTokenId(),
                tokenPair.refreshTokenExpiresIn()
        );
        return AuthenticationInfo.authenticated(clientType, tokenPair);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
    }

    private RefreshTokenClaims parseRefreshToken(
            String refreshToken,
            OAuthClientType expectedClientType
    ) {
        RefreshTokenClaims claims = tokenProvider.parseRefreshToken(refreshToken);
        if (claims.clientType() != expectedClientType) {
            throw new BusinessException(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS);
        }
        return claims;
    }
}
