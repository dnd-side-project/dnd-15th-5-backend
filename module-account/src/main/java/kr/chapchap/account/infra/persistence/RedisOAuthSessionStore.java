package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.application.info.OAuthAuthorizationSession;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthLoginSession;
import kr.chapchap.account.application.port.OAuthSessionStore;
import kr.chapchap.account.domain.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class RedisOAuthSessionStore implements OAuthSessionStore {

    private static final String STATE_KEY_PREFIX = "chapchap:account:oauth:state:";
    private static final String LOGIN_CODE_KEY_PREFIX = "chapchap:account:oauth:login-code:";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final Duration LOGIN_CODE_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    @Override
    public String createState(
            SocialProvider provider,
            OAuthClientType clientType,
            String codeChallenge
    ) {
        Objects.requireNonNull(provider, "소셜 로그인 제공자는 필수입니다.");
        Objects.requireNonNull(clientType, "OAuth 클라이언트 유형은 필수입니다.");
        Objects.requireNonNull(codeChallenge, "PKCE 코드 챌린지는 필수입니다.");

        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                STATE_KEY_PREFIX + state,
                provider.name() + ":" + clientType.name() + ":" + codeChallenge,
                STATE_TTL
        );
        return state;
    }

    @Override
    public Optional<OAuthAuthorizationSession> consumeState(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }

        String value = redisTemplate.opsForValue().getAndDelete(STATE_KEY_PREFIX + state);
        if (value == null) {
            return Optional.empty();
        }

        String[] session = value.split(":", 3);
        if (session.length != 3) {
            return Optional.empty();
        }

        try {
            return Optional.of(new OAuthAuthorizationSession(
                    SocialProvider.valueOf(session[0]),
                    OAuthClientType.valueOf(session[1]),
                    session[2]
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public String createLoginCode(
            Long userId,
            OAuthClientType clientType,
            String codeChallenge
    ) {
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(clientType, "OAuth 클라이언트 유형은 필수입니다.");
        Objects.requireNonNull(codeChallenge, "PKCE 코드 챌린지는 필수입니다.");

        String loginCode = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                createLoginCodeKey(loginCode, codeChallenge),
                userId + ":" + clientType.name(),
                LOGIN_CODE_TTL
        );
        return loginCode;
    }

    @Override
    public Optional<OAuthLoginSession> consumeLoginCode(
            String loginCode,
            String codeChallenge
    ) {
        if (loginCode == null || loginCode.isBlank()
                || codeChallenge == null || codeChallenge.isBlank()) {
            return Optional.empty();
        }

        String value = redisTemplate.opsForValue().getAndDelete(
                createLoginCodeKey(loginCode, codeChallenge)
        );
        if (value == null) {
            return Optional.empty();
        }

        String[] session = value.split(":", 2);
        if (session.length != 2) {
            return Optional.empty();
        }

        try {
            return Optional.of(new OAuthLoginSession(
                    Long.valueOf(session[0]),
                    OAuthClientType.valueOf(session[1])
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String createLoginCodeKey(String loginCode, String codeChallenge) {
        return LOGIN_CODE_KEY_PREFIX + loginCode + ":" + codeChallenge;
    }
}
