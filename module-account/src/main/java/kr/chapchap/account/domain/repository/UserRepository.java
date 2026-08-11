package kr.chapchap.account.domain.repository;

import kr.chapchap.account.domain.entity.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    User save(User user);
}
