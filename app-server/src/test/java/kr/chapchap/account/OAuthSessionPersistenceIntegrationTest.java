package kr.chapchap.account;

import kr.chapchap.account.application.info.OAuthAuthorizationSession;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.OAuthLoginSession;
import kr.chapchap.account.application.port.OAuthSessionStore;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OAuthSessionPersistenceIntegrationTest {

    private static final String CODE_CHALLENGE =
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String WRONG_CODE_CHALLENGE =
            "hMKafcbhlTNVzUFrvaAOFBfMnpPK39BTPqPkLaz-jOI";

    private final OAuthSessionStore oauthSessionStore;

    @Autowired
    OAuthSessionPersistenceIntegrationTest(OAuthSessionStore oauthSessionStore) {
        this.oauthSessionStore = oauthSessionStore;
    }

    @Test
    void OAuth_state는_clientType과_codeChallenge를_한_번만_반환한다() {
        // given
        String state = oauthSessionStore.createState(OAuthClientType.APP, CODE_CHALLENGE);

        // when
        OAuthAuthorizationSession firstSession = oauthSessionStore.consumeState(state).orElseThrow();

        // then
        assertThat(firstSession.clientType()).isEqualTo(OAuthClientType.APP);
        assertThat(firstSession.codeChallenge()).isEqualTo(CODE_CHALLENGE);
        assertThat(oauthSessionStore.consumeState(state)).isEmpty();
    }

    @Test
    void loginCode는_사용자와_clientType을_올바른_codeChallenge로_한_번만_반환한다() {
        // given
        String loginCode = oauthSessionStore.createLoginCode(
                1L,
                OAuthClientType.APP,
                CODE_CHALLENGE
        );

        // when & then
        assertThat(oauthSessionStore.consumeLoginCode(loginCode, WRONG_CODE_CHALLENGE))
                .isEmpty();
        assertThat(oauthSessionStore.consumeLoginCode(loginCode, CODE_CHALLENGE))
                .contains(new OAuthLoginSession(1L, OAuthClientType.APP));
        assertThat(oauthSessionStore.consumeLoginCode(loginCode, CODE_CHALLENGE))
                .isEmpty();
    }
}
