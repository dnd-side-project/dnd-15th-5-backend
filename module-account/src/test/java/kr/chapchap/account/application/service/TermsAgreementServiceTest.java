package kr.chapchap.account.application.service;

import kr.chapchap.account.application.command.TermsAgreementCommand;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.info.TokenPair;
import kr.chapchap.account.domain.entity.TermsType;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.entity.UserTermsAgreement;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.domain.repository.UserTermsAgreementRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TermsAgreementServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTermsAgreementRepository userTermsAgreementRepository;

    @Mock
    private LoginTokenService loginTokenService;

    @Captor
    private ArgumentCaptor<List<UserTermsAgreement>> agreementsCaptor;

    @InjectMocks
    private TermsAgreementService termsAgreementService;

    @Test
    void 필수_약관에_모두_동의하면_동의_이력을_저장하고_사용자를_활성화한_뒤_토큰을_발급한다() {
        // given
        User user = createUserWithId();
        TermsAgreementCommand command = new TermsAgreementCommand(
                USER_ID,
                OAuthClientType.WEB,
                true,
                true
        );
        AuthenticationInfo authenticationInfo = createAuthenticationInfo();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(loginTokenService.issueForActiveUser(USER_ID, OAuthClientType.WEB))
                .willReturn(authenticationInfo);

        // when
        AuthenticationInfo result = termsAgreementService.agree(command);

        // then
        then(userTermsAgreementRepository).should().saveAll(agreementsCaptor.capture());

        List<UserTermsAgreement> agreements = agreementsCaptor.getValue();
        LocalDateTime agreedAt = agreements.getFirst().getAgreedAt();
        assertThat(agreedAt).isNotNull();
        assertThat(agreements)
                .extracting(UserTermsAgreement::getTermsType)
                .containsExactlyInAnyOrder(TermsType.SERVICE_TERMS, TermsType.PRIVACY_POLICY);
        assertThat(agreements)
                .allSatisfy(agreement -> {
                    assertThat(agreement.getUserId()).isEqualTo(USER_ID);
                    assertThat(agreement.getTermsVersion()).isEqualTo("1.0");
                    assertThat(agreement.getAgreedAt()).isEqualTo(agreedAt);
                });
        assertThat(user.isActive()).isTrue();
        assertThat(result).isSameAs(authenticationInfo);
        then(loginTokenService).should().issueForActiveUser(USER_ID, OAuthClientType.WEB);
    }

    @ParameterizedTest
    @CsvSource({
            "false, true",
            "true, false"
    })
    void 필수_약관이_하나라도_누락되면_동의_이력을_저장하거나_사용자를_활성화하지_않는다(
            boolean serviceTermsAgreed,
            boolean privacyPolicyAgreed
    ) {
        // given
        TermsAgreementCommand command = new TermsAgreementCommand(
                USER_ID,
                OAuthClientType.WEB,
                serviceTermsAgreed,
                privacyPolicyAgreed
        );

        // when & then
        assertThatThrownBy(() -> termsAgreementService.agree(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
        then(userRepository).shouldHaveNoInteractions();
        then(userTermsAgreementRepository).shouldHaveNoInteractions();
        then(loginTokenService).shouldHaveNoInteractions();
    }

    @Test
    void 이미_가입을_완료한_사용자는_약관에_다시_동의할_수_없다() {
        // given
        User activeUser = createUserWithId();
        activeUser.completeTermsAgreement();
        TermsAgreementCommand command = new TermsAgreementCommand(
                USER_ID,
                OAuthClientType.WEB,
                true,
                true
        );

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(activeUser));

        // when & then
        assertThatThrownBy(() -> termsAgreementService.agree(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED)
                );
        then(userTermsAgreementRepository).shouldHaveNoInteractions();
        then(loginTokenService).shouldHaveNoInteractions();
    }

    private User createUserWithId() {
        User user = User.create("참참이");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private AuthenticationInfo createAuthenticationInfo() {
        TokenPair tokenPair = new TokenPair(
                "access-token",
                "refresh-token",
                "refresh-token-id",
                Duration.ofDays(14)
        );
        return AuthenticationInfo.authenticated(OAuthClientType.WEB, tokenPair);
    }
}
