package kr.chapchap.account.infra.security;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final String SCOPE_CLAIM = "scope";
    private static final String USER_SCOPE = "user";
    private static final OAuth2Error INVALID_USER_ERROR = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "활성 상태가 아닌 사용자입니다.",
            null
    );

    private final UserRepository userRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!USER_SCOPE.equals(jwt.getClaimAsString(SCOPE_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }

        try {
            Long userId = Long.valueOf(jwt.getSubject());
            boolean activeUser = userRepository.findById(userId)
                    .filter(User::isActive)
                    .isPresent();
            return activeUser
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(INVALID_USER_ERROR);
        } catch (NumberFormatException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_USER_ERROR);
        }
    }
}
