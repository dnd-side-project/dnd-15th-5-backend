package kr.chapchap.report.domain.repository;

import kr.chapchap.report.domain.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByUserIdAndReportMonth(Long userId, LocalDate reportMonth);
}
