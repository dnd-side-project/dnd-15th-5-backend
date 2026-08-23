package kr.chapchap.account.application.service;

import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.domain.service.NicknameGenerator;
import kr.chapchap.account.exception.AccountErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    void 소셜_사용자_식별값이_비어_있으면_Repository를_조회하지_않는다() {
        // given
        String providerUserId = " ";

        // when & then
        assertThatThrownBy(() -> socialLoginService.login(
                SocialProvider.KAKAO,
                providerUserId
        ))
                .isInstanceOf(IllegalArgumentException.class);
        then(socialAccountRepository).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
        then(nicknameGenerator).shouldHaveNoInteractions();
    }

    @Test
    void 탈퇴한_소셜_사용자가_로그인하면_탈퇴_계정_예외를_발생시킨다() {
        // given
        SocialAccount socialAccount = SocialAccount.create(
                1L,
                SocialProvider.GOOGLE,
                "google-sub"
        );
        User user = User.create("테스트");
        user.completeTermsAgreement();
        user.withdraw(LocalDateTime.of(2026, 8, 23, 21, 7));
        given(socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.GOOGLE,
                "google-sub"
        )).willReturn(Optional.of(socialAccount));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> socialLoginService.login(
                SocialProvider.GOOGLE,
                "google-sub"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(AccountErrorCode.ACCOUNT_WITHDRAWN)
        );
    }
}
