package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.domain.entity.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserTermsAgreementJpaRepository extends JpaRepository<UserTermsAgreement, Long> {
}
