package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.domain.entity.UserTermsAgreement;
import kr.chapchap.account.domain.repository.UserTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class UserTermsAgreementRepositoryAdapter implements UserTermsAgreementRepository {

    private final UserTermsAgreementJpaRepository userTermsAgreementJpaRepository;

    @Override
    public List<UserTermsAgreement> saveAll(List<UserTermsAgreement> agreements) {
        return userTermsAgreementJpaRepository.saveAll(agreements);
    }
}
