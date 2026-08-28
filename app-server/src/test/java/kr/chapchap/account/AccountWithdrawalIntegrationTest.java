package kr.chapchap.account;

import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.application.port.TokenProvider;
import kr.chapchap.account.application.service.WithdrawnUserCleanupService;
import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
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
    private static final String GOOGLE_PLACE_ID = "withdrawal-place";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenProvider tokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final WithdrawnUserCleanupService withdrawnUserCleanupService;
    private final Set<Long> refreshTokenUserIds = new HashSet<>();

    @MockitoBean
    private KakaoAuthenticationPort kakaoAuthenticationPort;

    @MockitoBean
    private ProfileImageStorage profileImageStorage;

    @MockitoBean
    private ReceiptImageStorage receiptImageStorage;

    @Autowired
    AccountWithdrawalIntegrationTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            RefreshTokenStore refreshTokenStore,
            TokenProvider tokenProvider,
            JdbcTemplate jdbcTemplate,
            WithdrawnUserCleanupService withdrawnUserCleanupService
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenProvider = tokenProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.withdrawnUserCleanupService = withdrawnUserCleanupService;
    }

    @AfterEach
    void cleanUp() {
        refreshTokenUserIds.forEach(refreshTokenStore::revokeAll);
        jdbcTemplate.update("DELETE FROM social_accounts");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM places WHERE google_place_id = ?", GOOGLE_PLACE_ID);
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

    @Test
    void 탈퇴한_회원의_S3_파일과_사용자_데이터를_삭제하고_장소는_유지한다() {
        // given
        User user = saveActiveKakaoUser();
        Long placeId = jdbcTemplate.queryForObject("""
                INSERT INTO places (
                    google_place_id, name, road_address, administrative_dong_code,
                    administrative_dong_name, location
                ) VALUES (
                    ?, '탈퇴 테스트 장소', '서울특별시 성동구', '11200690',
                    '성수2가3동', ST_SetSRID(ST_MakePoint(127.05, 37.54), 4326)::geography
                ) RETURNING id
                """, Long.class, GOOGLE_PLACE_ID);
        Long consumptionId = jdbcTemplate.queryForObject("""
                INSERT INTO consumptions (
                    purchase_date, purchase_time, amount, category, user_id, place_id, sticker_item_id
                ) VALUES (CURRENT_DATE, TIME '12:00:00', 10000, '카페', ?, ?, 1)
                RETURNING id
                """, Long.class, user.getId(), placeId);
        Long reportId = jdbcTemplate.queryForObject("""
                INSERT INTO report (
                    user_id, report_month, persona_type, total_visit_count,
                    new_town_count, new_place_count, new_sticker_count
                ) VALUES (?, DATE '2026-08-01', 'RHMP', 1, 1, 1, 1)
                RETURNING id
                """, Long.class, user.getId());
        jdbcTemplate.update("""
                INSERT INTO user_terms_agreements (
                    terms_type, terms_version, agreed_at, user_id
                ) VALUES ('SERVICE_TERMS', '1.0', CURRENT_TIMESTAMP, ?)
                """, user.getId());
        jdbcTemplate.update("""
                INSERT INTO receipt_images (
                    object_key, content_type, file_size_bytes, status,
                    attached_at, user_id, consumption_id
                ) VALUES ('receipts/withdrawal/receipt', 'image/png', 100, 'ATTACHED',
                    CURRENT_TIMESTAMP, ?, ?)
                """, user.getId(), consumptionId);
        jdbcTemplate.update(
                "INSERT INTO place_likes (user_id, place_id) VALUES (?, ?)",
                user.getId(),
                placeId
        );
        jdbcTemplate.update("""
                INSERT INTO notifications (
                    user_id, type, title, body, push_status
                ) VALUES (?, 'FRIDAY_REMINDER', '탈퇴 테스트 알림', '탈퇴 테스트 알림 내용', 'SENT')
                """, user.getId());
        jdbcTemplate.update("""
                INSERT INTO report_category_stat (report_id, category, percentage)
                VALUES (?, '카페', 100)
                """, reportId);
        jdbcTemplate.update("""
                INSERT INTO report_town_rank (report_id, rank, town_name, visit_count)
                VALUES (?, 1, '성수2가3동', 1)
                """, reportId);
        jdbcTemplate.update("""
                INSERT INTO report_place_rank (
                    report_id, rank, place_id, place_name, visit_count
                ) VALUES (?, 1, ?, '탈퇴 테스트 장소', 1)
                """, reportId, placeId);
        jdbcTemplate.update("""
                INSERT INTO report_time_pattern (
                    report_id, day_of_week, visit_hour, visit_count
                ) VALUES (?, 1, 12, 1)
                """, reportId);
        user.withdraw(LocalDateTime.now());
        userRepository.save(user);
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return null;
        }).given(profileImageStorage).deleteAllByUserId(user.getId());
        willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return null;
        }).given(receiptImageStorage).deleteAllByUserId(user.getId());

        // when
        int deletedCount = withdrawnUserCleanupService.cleanupWithdrawnUsers();

        // then
        assertThat(deletedCount).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_accounts WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_terms_agreements WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consumptions WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM receipt_images WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_category_stat WHERE report_id = ?",
                Integer.class,
                reportId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_town_rank WHERE report_id = ?",
                Integer.class,
                reportId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_place_rank WHERE report_id = ?",
                Integer.class,
                reportId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_time_pattern WHERE report_id = ?",
                Integer.class,
                reportId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM place_likes WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ?",
                Integer.class,
                user.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM places WHERE id = ?",
                Integer.class,
                placeId
        )).isOne();
        then(profileImageStorage).should().deleteAllByUserId(user.getId());
        then(receiptImageStorage).should().deleteAllByUserId(user.getId());
    }

    @Test
    void S3_삭제에_실패한_회원은_WITHDRAWN_상태로_남겨_다음_실행에서_재시도한다() {
        // given
        User user = saveActiveKakaoUser();
        user.withdraw(LocalDateTime.now());
        userRepository.save(user);
        willThrow(new IllegalStateException("S3 일시 오류"))
                .willDoNothing()
                .given(profileImageStorage)
                .deleteAllByUserId(user.getId());

        // when
        int firstDeletedCount = withdrawnUserCleanupService.cleanupWithdrawnUsers();

        // then
        assertThat(firstDeletedCount).isZero();
        assertThat(findUserData(user.getId()).get("status")).isEqualTo("WITHDRAWN");
        then(receiptImageStorage).shouldHaveNoInteractions();

        // when
        int retryDeletedCount = withdrawnUserCleanupService.cleanupWithdrawnUsers();

        // then
        assertThat(retryDeletedCount).isOne();
        assertThat(userRepository.findById(user.getId())).isEmpty();
        then(profileImageStorage).should(times(2)).deleteAllByUserId(user.getId());
        then(receiptImageStorage).should().deleteAllByUserId(user.getId());
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
