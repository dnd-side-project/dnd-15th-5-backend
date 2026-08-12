package kr.chapchap.account;

import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.application.port.TokenProvider;
import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AccountWithdrawalIntegrationTest {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final String PROFILE_IMAGE_KEY = "profiles/withdrawal/profile-image-key";
    private static final String PROVIDER_USER_ID = "withdrawal-kakao-user";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenProvider tokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final Set<Long> refreshTokenUserIds = new HashSet<>();

    @MockitoBean
    private KakaoAuthenticationPort kakaoAuthenticationPort;

    @MockitoBean
    private ProfileImageStorage profileImageStorage;

    @Autowired
    AccountWithdrawalIntegrationTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            RefreshTokenStore refreshTokenStore,
            TokenProvider tokenProvider,
            JdbcTemplate jdbcTemplate
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenProvider = tokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUp() {
        refreshTokenUserIds.forEach(refreshTokenStore::revokeAll);
        jdbcTemplate.update("DELETE FROM social_accounts");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 회원_탈퇴하면_상태와_탈퇴_시각을_반영하고_모든_인증_토큰을_무효화한다()
            throws Exception {
        // given
        User user = saveActiveKakaoUser();
        TokenPair tokenPair = tokenProvider.issueUserTokens(user.getId(), OAuthClientType.WEB);
        String anotherRefreshTokenId = UUID.randomUUID().toString();
        saveRefreshToken(user.getId(), tokenPair.refreshTokenId());
        saveRefreshToken(user.getId(), anotherRefreshTokenId);

        Long otherUserId = user.getId() + 1L;
        String otherUserRefreshTokenId = UUID.randomUUID().toString();
        saveRefreshToken(otherUserId, otherUserRefreshTokenId);

        // when
        mockMvc.perform(delete("/accounts/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenPair.accessToken()
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(cookie().maxAge("refresh_token", 0));

        // then
        Map<String, Object> userData = findUserData(user.getId());
        assertThat(userData.get("status")).isEqualTo("WITHDRAWN");
        assertThat(userData.get("withdrawn_at")).isNotNull();
        assertThat(userData.get("profile_image_key")).isEqualTo(PROFILE_IMAGE_KEY);
        assertThat(refreshTokenStore.consume(user.getId(), tokenPair.refreshTokenId()))
                .isFalse();
        assertThat(refreshTokenStore.consume(user.getId(), anotherRefreshTokenId))
                .isFalse();
        assertThat(refreshTokenStore.consume(otherUserId, otherUserRefreshTokenId))
                .isTrue();
        then(kakaoAuthenticationPort).should().unlink(PROVIDER_USER_ID);

        mockMvc.perform(get("/accounts/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenPair.accessToken()
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));
    }

    private User saveActiveKakaoUser() {
        User user = User.create("탈퇴회원");
        user.completeTermsAgreement();
        user.updateProfileImageKey(PROFILE_IMAGE_KEY);
        User savedUser = userRepository.save(user);
        socialAccountRepository.save(SocialAccount.create(
                savedUser.getId(),
                SocialProvider.KAKAO,
                PROVIDER_USER_ID
        ));
        return savedUser;
    }

    private void saveRefreshToken(Long userId, String tokenId) {
        refreshTokenUserIds.add(userId);
        refreshTokenStore.save(userId, tokenId, REFRESH_TOKEN_TTL);
    }

    private Map<String, Object> findUserData(Long userId) {
        return jdbcTemplate.queryForMap(
                "SELECT status, withdrawn_at, profile_image_key FROM users WHERE id = ?",
                userId
        );
    }
}
