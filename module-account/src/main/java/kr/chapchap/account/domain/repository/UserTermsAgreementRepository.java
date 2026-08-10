package kr.chapchap.account.domain.repository;

import kr.chapchap.account.domain.entity.UserTermsAgreement;

import java.util.List;

public interface UserTermsAgreementRepository {

    List<UserTermsAgreement> saveAll(List<UserTermsAgreement> agreements);
}
