package kr.chapchap.account.domain.repository;

import kr.chapchap.account.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    List<Long> findWithdrawnUserIds();

    User save(User user);

    void delete(User user);
}
