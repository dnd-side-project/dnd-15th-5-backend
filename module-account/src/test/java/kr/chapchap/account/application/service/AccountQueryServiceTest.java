package kr.chapchap.account.application.service;

import kr.chapchap.account.application.info.AccountInfo;
import kr.chapchap.account.application.port.ProfileImageStorage;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final String NICKNAME = "찹찹이";
    private static final String PROFILE_IMAGE_KEY = "profiles/1/profile.png";
    private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileImageStorage profileImageStorage;

    @InjectMocks
    private AccountQueryService accountQueryService;

    @Test
    void 활성_사용자의_내_정보를_조회한다() {
        // given
        User user = createActiveUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(profileImageStorage.createReadUrl(PROFILE_IMAGE_KEY))
                .willReturn(PROFILE_IMAGE_URL);

        // when
        AccountInfo result = accountQueryService.getAccount(USER_ID);

        // then
        assertThat(result).isEqualTo(new AccountInfo(
                USER_ID,
                NICKNAME,
                PROFILE_IMAGE_URL
        ));
        then(profileImageStorage).should().createReadUrl(PROFILE_IMAGE_KEY);
    }

    @Test
    void 프로필_이미지가_없으면_조회_URL을_생성하지_않는다() {
        // given
        User user = createActiveUser();
        ReflectionTestUtils.setField(user, "profileImageKey", null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        AccountInfo result = accountQueryService.getAccount(USER_ID);

        // then
        assertThat(result.profileImageUrl()).isNull();
        then(profileImageStorage).shouldHaveNoInteractions();
    }

    @Test
    void 인증_정보에_해당하는_사용자가_없으면_인증_오류가_발생한다() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountQueryService.getAccount(USER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS)
                );
    }

    @Test
    void 활성_상태가_아닌_사용자는_내_정보를_조회할_수_없다() {
        // given
        User user = User.create(NICKNAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> accountQueryService.getAccount(USER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.ACCESS_DENIED)
                );
        then(profileImageStorage).shouldHaveNoInteractions();
    }

    private User createActiveUser() {
        User user = User.create(NICKNAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ReflectionTestUtils.setField(user, "profileImageKey", PROFILE_IMAGE_KEY);
        user.completeTermsAgreement();
        return user;
    }
}
