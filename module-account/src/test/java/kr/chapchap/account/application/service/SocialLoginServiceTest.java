package kr.chapchap.account.application.service;

import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.domain.service.NicknameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
