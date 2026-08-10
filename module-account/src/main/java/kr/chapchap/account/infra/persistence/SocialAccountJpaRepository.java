package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SocialAccountJpaRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}
