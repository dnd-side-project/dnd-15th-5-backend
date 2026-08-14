package kr.chapchap.account;

import kr.chapchap.account.application.command.TermsAgreementCommand;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.service.SocialLoginService;
import kr.chapchap.account.application.service.TermsAgreementService;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SocialLoginPersistenceIntegrationTest {

    private final SocialLoginService socialLoginService;
    private final TermsAgreementService termsAgreementService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    SocialLoginPersistenceIntegrationTest(
            SocialLoginService socialLoginService,
            TermsAgreementService termsAgreementService,
            JdbcTemplate jdbcTemplate
    ) {
        this.socialLoginService = socialLoginService;
        this.termsAgreementService = termsAgreementService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void 필수_약관에_동의하면_이력을_저장하고_사용자를_활성화한다() {
        // given
        Long userId = socialLoginService.login(
                SocialProvider.KAKAO,
                "terms-user"
        );

        // when
        AuthenticationInfo authenticationInfo = termsAgreementService.agree(
                new TermsAgreementCommand(
                        userId,
                        OAuthClientType.WEB,
                        true,
                        true
                )
        );

        // then
        assertThat(authenticationInfo.clientType()).isEqualTo(OAuthClientType.WEB);
        assertThat(authenticationInfo.requiresTermsAgreement()).isFalse();
        assertThat(authenticationInfo.accessToken()).isNotBlank();
        assertThat(authenticationInfo.refreshToken()).isNotBlank();
        assertThat(findUserStatus(userId)).isEqualTo("ACTIVE");
        assertThat(countTermsAgreements(userId)).isEqualTo(2L);
    }

    @AfterEach
    void 데이터베이스를_정리한다() {
        jdbcTemplate.update("DELETE FROM social_accounts");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 처음_소셜_로그인하면_계정을_생성하고_같은_식별자로_다시_로그인하면_재사용한다() {
        // given
        String providerUserId = "123456789";

        // when
        Long firstUserId = socialLoginService.login(SocialProvider.KAKAO, providerUserId);
        Long secondUserId = socialLoginService.login(SocialProvider.KAKAO, providerUserId);

        // then
        assertThat(secondUserId).isEqualTo(firstUserId);
        assertThat(findUserStatus(firstUserId)).isEqualTo("PENDING_TERMS");
        assertThat(countUsers()).isEqualTo(1L);
        assertThat(countSocialAccounts()).isEqualTo(1L);
        Map<String, Object> userData = findUserData(firstUserId);
        assertThat(userData.get("profile_image_key")).isNull();
        assertThat(userData.get("email")).isNull();
    }

    @Test
    void Google_sub로_로그인하면_GOOGLE_소셜_계정을_저장한다() {
        // given
        String googleSubject = "google-subject";

        // when
        Long userId = socialLoginService.login(SocialProvider.GOOGLE, googleSubject);

        // then
        Map<String, Object> socialAccount = jdbcTemplate.queryForMap(
                "SELECT provider, provider_user_id FROM social_accounts WHERE user_id = ?",
                userId
        );
        assertThat(socialAccount.get("provider")).isEqualTo("GOOGLE");
        assertThat(socialAccount.get("provider_user_id")).isEqualTo(googleSubject);
    }

    private long countUsers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }

    private long countSocialAccounts() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM social_accounts", Long.class);
    }

    private String findUserStatus(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?",
                String.class,
                userId
        );
    }

    private Map<String, Object> findUserData(Long userId) {
        return jdbcTemplate.queryForMap(
                "SELECT profile_image_key, email FROM users WHERE id = ?",
                userId
        );
    }

    private long countTermsAgreements(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_terms_agreements WHERE user_id = ?",
                Long.class,
                userId
        );
    }
}
