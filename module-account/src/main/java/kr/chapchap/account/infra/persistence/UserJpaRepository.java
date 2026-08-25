package kr.chapchap.account.infra.persistence;

import jakarta.persistence.LockModeType;
import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface UserJpaRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT u.id
            FROM User u
            WHERE u.status = :status
            ORDER BY u.id
            """)
    List<Long> findIdsByStatus(@Param("status") UserStatus status);
}
