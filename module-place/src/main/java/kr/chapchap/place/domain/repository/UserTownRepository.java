package kr.chapchap.place.domain.repository;

import kr.chapchap.place.domain.entity.UserTown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTownRepository extends JpaRepository<UserTown, Long> {

    Optional<UserTown> findByUserIdAndAdministrativeDongCode(Long userId, String administrativeDongCode);

    List<UserTown> findByUserIdOrderBySelectedAtDesc(Long userId);

    Optional<UserTown> findByIdAndUserId(Long id, Long userId);
}
