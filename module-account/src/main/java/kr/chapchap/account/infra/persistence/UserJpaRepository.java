package kr.chapchap.account.infra.persistence;

import kr.chapchap.account.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long> {
}
