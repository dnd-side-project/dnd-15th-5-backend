package kr.chapchap.receipt.domain.repository;

import kr.chapchap.receipt.domain.entity.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {
}
