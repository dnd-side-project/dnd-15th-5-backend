package kr.chapchap.account.application.service;

import kr.chapchap.account.application.command.AccountUpdateCommand;
import kr.chapchap.account.application.event.ProfileImageCleanupEvent;
import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.port.GoogleAuthenticationPort;
import kr.chapchap.account.application.port.KakaoAuthenticationPort;
import kr.chapchap.account.application.port.OAuthClientRedirectPort;
import kr.chapchap.account.application.port.OAuthSessionStore;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.application.port.RefreshTokenStore;
import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.entity.UserStatus;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccountCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final String NICKNAME = "찹찹이";
    private static final String UPDATED_NICKNAME = "새찹찹이";
    private static final String PROFILE_IMAGE_KEY = "profiles/1/new-image-key";
    private static final String PREVIOUS_PROFILE_IMAGE_KEY = "profiles/1/previous-image-key";
    private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";
    private static final String KAKAO_PROVIDER_USER_ID = "123456789";
    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileImageStorage profileImageStorage;

    @Mock
    private ProfileImageValidator profileImageValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private KakaoAuthenticationPort kakaoAuthenticationPort;

    @Mock
    private GoogleAuthenticationPort googleAuthenticationPort;

    @Mock
    private OAuthSessionStore oauthSessionStore;

    @Mock
    private OAuthClientRedirectPort oauthClientRedirectPort;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AccountCommandService accountCommandService;

    @InjectMocks
    private AccountWithdrawalService accountWithdrawalService;

    @Test
    void 닉네임만_수정한다() {
        // given
        User user = createActiveUser(null);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                UPDATED_NICKNAME,
                null,
                false
        );

        // when
        AccountInfo result = accountCommandService.updateAccount(command);

        // then
        assertThat(user.getNickname()).isEqualTo(UPDATED_NICKNAME);
        assertThat(result).isEqualTo(new AccountInfo(USER_ID, UPDATED_NICKNAME, null));
        then(userRepository).should().findByIdForUpdate(USER_ID);
        then(profileImageStorage).shouldHaveNoInteractions();
    }

    @Test
    void 닉네임과_프로필_이미지를_함께_수정한다() {
        // given
        User user = createActiveUser(null);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(profileImageValidator.validateAndGetContentType(PNG_IMAGE)).willReturn("image/png");
        given(profileImageStorage.store(USER_ID, PNG_IMAGE, "image/png"))
                .willReturn(PROFILE_IMAGE_KEY);
        given(profileImageStorage.createReadUrl(PROFILE_IMAGE_KEY))
                .willReturn(PROFILE_IMAGE_URL);
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                UPDATED_NICKNAME,
                PNG_IMAGE,
                false
        );

        // when
        AccountInfo result = accountCommandService.updateAccount(command);

        // then
        assertThat(user.getNickname()).isEqualTo(UPDATED_NICKNAME);
        assertThat(user.getProfileImageKey()).isEqualTo(PROFILE_IMAGE_KEY);
        assertThat(result).isEqualTo(new AccountInfo(
                USER_ID,
                UPDATED_NICKNAME,
                PROFILE_IMAGE_URL
        ));
        then(profileImageValidator).should().validateAndGetContentType(PNG_IMAGE);
        then(profileImageStorage).should().store(USER_ID, PNG_IMAGE, "image/png");
        then(eventPublisher).should().publishEvent(new ProfileImageCleanupEvent(
                PROFILE_IMAGE_KEY,
                null
        ));
    }

    @Test
    void 프로필_이미지를_삭제한다() {
        // given
        User user = createActiveUser(PREVIOUS_PROFILE_IMAGE_KEY);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                null,
                null,
                true
        );

        // when
        AccountInfo result = accountCommandService.updateAccount(command);

        // then
        assertThat(user.getProfileImageKey()).isNull();
        assertThat(result.profileImageUrl()).isNull();
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).should().publishEvent(new ProfileImageCleanupEvent(
                null,
                PREVIOUS_PROFILE_IMAGE_KEY
        ));
    }

    @Test
    void 이미지_파일과_삭제를_동시에_요청할_수_없다() {
        // given
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                null,
                PNG_IMAGE,
                true
        );

        // when & then
        assertThatThrownBy(() -> accountCommandService.updateAccount(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.PROFILE_IMAGE_UPDATE_CONFLICT)
                );
        then(userRepository).shouldHaveNoInteractions();
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 수정할_값이_없으면_수정할_수_없다() {
        // given
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                null,
                null,
                false
        );

        // when & then
        assertThatThrownBy(() -> accountCommandService.updateAccount(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.ACCOUNT_UPDATE_VALUE_REQUIRED)
                );
        then(userRepository).shouldHaveNoInteractions();
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 유효하지_않은_프로필_이미지는_저장할_수_없다() {
        // given
        byte[] invalidImage = new byte[]{1, 2, 3};
        given(profileImageValidator.validateAndGetContentType(invalidImage))
                .willThrow(new BusinessException(AccountErrorCode.INVALID_PROFILE_IMAGE));
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                null,
                invalidImage,
                false
        );

        // when & then
        assertThatThrownBy(() -> accountCommandService.updateAccount(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AccountErrorCode.INVALID_PROFILE_IMAGE)
                );
        then(userRepository).shouldHaveNoInteractions();
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 인증_정보에_해당하는_사용자가_없으면_수정할_수_없다() {
        // given
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                UPDATED_NICKNAME,
                null,
                false
        );

        // when & then
        assertThatThrownBy(() -> accountCommandService.updateAccount(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
        );
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 활성_상태가_아닌_사용자는_수정할_수_없다() {
        // given
        User user = User.create(NICKNAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        AccountUpdateCommand command = new AccountUpdateCommand(
                USER_ID,
                UPDATED_NICKNAME,
                null,
                false
        );

        // when & then
        assertThatThrownBy(() -> accountCommandService.updateAccount(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED)
        );
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 활성_사용자를_탈퇴시키고_카카오_연결과_Refresh_Token을_정리한다() {
        // given
        User user = createActiveUser(PREVIOUS_PROFILE_IMAGE_KEY);
        SocialAccount socialAccount = SocialAccount.create(
                USER_ID,
                SocialProvider.KAKAO,
                KAKAO_PROVIDER_USER_ID
        );
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.KAKAO
        )).willReturn(Optional.of(socialAccount));

        // when
        accountWithdrawalService.startWithdrawal(USER_ID, OAuthClientType.WEB);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getWithdrawnAt()).isNotNull();
        assertThat(user.getProfileImageKey()).isEqualTo(PREVIOUS_PROFILE_IMAGE_KEY);
        then(kakaoAuthenticationPort).should().unlink(KAKAO_PROVIDER_USER_ID);
        then(refreshTokenStore).should().revokeAll(USER_ID);
        then(profileImageStorage).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 인증_정보에_해당하는_사용자가_없으면_탈퇴할_수_없다() {
        // given
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountWithdrawalService.startWithdrawal(
                USER_ID,
                OAuthClientType.WEB
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
                );
        then(socialAccountRepository).shouldHaveNoInteractions();
        then(kakaoAuthenticationPort).shouldHaveNoInteractions();
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    @Test
    void 활성_상태가_아닌_사용자는_탈퇴할_수_없다() {
        // given
        User user = User.create(NICKNAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> accountWithdrawalService.startWithdrawal(
                USER_ID,
                OAuthClientType.WEB
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED)
                );
        then(socialAccountRepository).shouldHaveNoInteractions();
        then(kakaoAuthenticationPort).shouldHaveNoInteractions();
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    @Test
    void 카카오_연결_해제에_실패하면_탈퇴와_토큰_정리를_진행하지_않는다() {
        // given
        User user = createActiveUser(null);
        SocialAccount socialAccount = SocialAccount.create(
                USER_ID,
                SocialProvider.KAKAO,
                KAKAO_PROVIDER_USER_ID
        );
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProvider(
                USER_ID,
                SocialProvider.KAKAO
        )).willReturn(Optional.of(socialAccount));
        willThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE))
                .given(kakaoAuthenticationPort)
                .unlink(KAKAO_PROVIDER_USER_ID);

        // when & then
        assertThatThrownBy(() -> accountWithdrawalService.startWithdrawal(
                USER_ID,
                OAuthClientType.WEB
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE)
                );
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getWithdrawnAt()).isNull();
        then(refreshTokenStore).shouldHaveNoInteractions();
    }

    private User createActiveUser(String profileImageKey) {
        User user = User.create(NICKNAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ReflectionTestUtils.setField(user, "profileImageKey", profileImageKey);
        user.completeTermsAgreement();
        return user;
    }

}
