package kr.chapchap.account;

import kr.chapchap.account.application.command.AccountUpdateCommand;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.application.service.AccountCommandService;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AccountUpdatePersistenceIntegrationTest {

    private static final String PREVIOUS_OBJECT_KEY = "profiles/1/previous-image-key";
    private static final String NEW_OBJECT_KEY = "profiles/1/new-image-key";
    private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";
    private static final int ASYNC_TIMEOUT_SECONDS = 5;
    private static final int LOCK_WAIT_TIMEOUT_MILLIS = 500;
    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    private final AccountCommandService accountCommandService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ProfileImageStorage profileImageStorage;

    @Autowired
    AccountUpdatePersistenceIntegrationTest(
            AccountCommandService accountCommandService,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.accountCommandService = accountCommandService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 이미지_교체가_커밋되면_DB_Key를_변경하고_기존_이미지를_삭제한다() {
        // given
        User user = saveActiveUserWithProfileImage();
        given(profileImageStorage.store(user.getId(), PNG_IMAGE, "image/png"))
                .willReturn(NEW_OBJECT_KEY);
        given(profileImageStorage.createReadUrl(NEW_OBJECT_KEY))
                .willReturn(PROFILE_IMAGE_URL);

        // when
        accountCommandService.updateAccount(new AccountUpdateCommand(
                user.getId(),
                "새찹찹이",
                PNG_IMAGE,
                false
        ));

        // then
        Map<String, Object> userData = findUserData(user.getId());
        assertThat(userData.get("nickname")).isEqualTo("새찹찹이");
        assertThat(userData.get("profile_image_key")).isEqualTo(NEW_OBJECT_KEY);
        then(profileImageStorage).should().delete(PREVIOUS_OBJECT_KEY);
    }

    @Test
    void 이미지_수정이_롤백되면_기존_DB_Key를_유지하고_새_이미지를_삭제한다() {
        // given
        User user = saveActiveUserWithProfileImage();
        given(profileImageStorage.store(user.getId(), PNG_IMAGE, "image/png"))
                .willReturn(NEW_OBJECT_KEY);
        given(profileImageStorage.createReadUrl(NEW_OBJECT_KEY))
                .willThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));

        // when & then
        assertThatThrownBy(() -> accountCommandService.updateAccount(new AccountUpdateCommand(
                user.getId(),
                "새찹찹이",
                PNG_IMAGE,
                false
        ))).isInstanceOf(BusinessException.class);

        Map<String, Object> userData = findUserData(user.getId());
        assertThat(userData.get("nickname")).isEqualTo("찹찹이");
        assertThat(userData.get("profile_image_key")).isEqualTo(PREVIOUS_OBJECT_KEY);
        then(profileImageStorage).should().delete(NEW_OBJECT_KEY);
    }

    @Test
    void 이미지_삭제가_커밋되면_DB_Key를_비우고_기존_이미지를_삭제한다() {
        // given
        User user = saveActiveUserWithProfileImage();

        // when
        accountCommandService.updateAccount(new AccountUpdateCommand(
                user.getId(),
                null,
                null,
                true
        ));

        // then
        assertThat(findUserData(user.getId()).get("profile_image_key")).isNull();
        then(profileImageStorage).should().delete(PREVIOUS_OBJECT_KEY);
    }

    @Test
    void 동일_사용자의_수정은_행_락으로_순차_처리한다() throws Exception {
        // given
        User user = saveActiveUserWithProfileImage();
        CountDownLatch firstStoreStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstStore = new CountDownLatch(1);
        CountDownLatch secondRequestStarted = new CountDownLatch(1);
        given(profileImageStorage.store(user.getId(), PNG_IMAGE, "image/png"))
                .willAnswer(invocation -> {
                    firstStoreStarted.countDown();
                    if (!releaseFirstStore.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("첫 번째 이미지 저장 대기가 시간 내에 해제되지 않았습니다.");
                    }
                    return NEW_OBJECT_KEY;
                });
        given(profileImageStorage.createReadUrl(PREVIOUS_OBJECT_KEY))
                .willReturn(PROFILE_IMAGE_URL);
        given(profileImageStorage.createReadUrl(NEW_OBJECT_KEY))
                .willReturn(PROFILE_IMAGE_URL);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<?> imageUpdate = executorService.submit(() ->
                    accountCommandService.updateAccount(new AccountUpdateCommand(
                            user.getId(),
                            null,
                            PNG_IMAGE,
                            false
                    ))
            );
            assertThat(firstStoreStarted.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            Future<?> nicknameUpdate = executorService.submit(() -> {
                secondRequestStarted.countDown();
                return accountCommandService.updateAccount(new AccountUpdateCommand(
                        user.getId(),
                        "새찹찹이",
                        null,
                        false
                ));
            });
            assertThat(secondRequestStarted.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // when & then
            assertThatThrownBy(() -> nicknameUpdate.get(
                    LOCK_WAIT_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS
            )).isInstanceOf(TimeoutException.class);

            releaseFirstStore.countDown();
            imageUpdate.get(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            nicknameUpdate.get(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Map<String, Object> userData = findUserData(user.getId());
            assertThat(userData.get("nickname")).isEqualTo("새찹찹이");
            assertThat(userData.get("profile_image_key")).isEqualTo(NEW_OBJECT_KEY);
            then(profileImageStorage).should().delete(PREVIOUS_OBJECT_KEY);
        } finally {
            releaseFirstStore.countDown();
            executorService.shutdownNow();
        }
    }

    private User saveActiveUserWithProfileImage() {
        User user = User.create("찹찹이");
        user.completeTermsAgreement();
        user.updateProfileImageKey(PREVIOUS_OBJECT_KEY);
        return userRepository.save(user);
    }

    private Map<String, Object> findUserData(Long userId) {
        return jdbcTemplate.queryForMap(
                "SELECT nickname, profile_image_key FROM users WHERE id = ?",
                userId
        );
    }
}
