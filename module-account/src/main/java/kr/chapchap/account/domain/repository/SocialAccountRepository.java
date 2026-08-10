package kr.chapchap.account.domain.repository;

import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;

import java.util.Optional;

public interface SocialAccountRepository {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    SocialAccount save(SocialAccount socialAccount);
}
