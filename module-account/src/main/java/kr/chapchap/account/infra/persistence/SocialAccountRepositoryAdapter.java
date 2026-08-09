package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.domain.entity.SocialAccount;
import kr.chapchap.account.domain.entity.SocialProvider;
import kr.chapchap.account.domain.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class SocialAccountRepositoryAdapter implements SocialAccountRepository {

    private final SocialAccountJpaRepository socialAccountJpaRepository;

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    ) {
        return socialAccountJpaRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        return socialAccountJpaRepository.save(socialAccount);
    }
}
