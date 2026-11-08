package kr.chapchap.report.domain.repository;

import kr.chapchap.report.domain.entity.ReportSummaryExposure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportSummaryExposureRepository extends JpaRepository<ReportSummaryExposure, Long> {

    Optional<ReportSummaryExposure> findByUserIdAndExposureDate(Long userId, LocalDate exposureDate);

    List<ReportSummaryExposure> findTop7ByUserIdOrderByExposureDateDesc(Long userId);
}
