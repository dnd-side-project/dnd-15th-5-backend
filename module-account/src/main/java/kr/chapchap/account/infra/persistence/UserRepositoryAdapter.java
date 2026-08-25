package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.entity.UserStatus;
import kr.chapchap.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return userJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public List<Long> findWithdrawnUserIds() {
        return userJpaRepository.findIdsByStatus(UserStatus.WITHDRAWN);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public void delete(User user) {
        userJpaRepository.delete(user);
    }
}
