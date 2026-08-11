package kr.chapchap.account.application.service;

import kr.chapchap.account.application.command.TermsAgreementCommand;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.domain.entity.TermsType;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.entity.UserTermsAgreement;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.account.domain.repository.UserTermsAgreementRepository;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TermsAgreementService {

    private static final String SERVICE_TERMS_VERSION = "1.0";
    private static final String PRIVACY_POLICY_VERSION = "1.0";

    private final UserRepository userRepository;
    private final UserTermsAgreementRepository userTermsAgreementRepository;
    private final LoginTokenService loginTokenService;

    @Transactional
    public AuthenticationInfo agree(TermsAgreementCommand command) {
        if (!command.serviceTermsAgreed() || !command.privacyPolicyAgreed()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_AUTHENTICATION_CREDENTIALS
                ));
        if (!user.isPendingTerms()) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        LocalDateTime agreedAt = LocalDateTime.now();
        userTermsAgreementRepository.saveAll(List.of(
                UserTermsAgreement.create(
                        user.getId(),
                        TermsType.SERVICE_TERMS,
                        SERVICE_TERMS_VERSION,
                        agreedAt
                ),
                UserTermsAgreement.create(
                        user.getId(),
                        TermsType.PRIVACY_POLICY,
                        PRIVACY_POLICY_VERSION,
                        agreedAt
                )
        ));
        user.completeTermsAgreement();

        return loginTokenService.issueForActiveUser(
                user.getId(),
                command.clientType()
        );
    }
}
