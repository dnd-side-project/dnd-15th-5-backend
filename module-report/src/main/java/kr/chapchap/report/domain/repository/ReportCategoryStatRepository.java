package kr.chapchap.report.domain.repository;

import kr.chapchap.report.domain.entity.ReportCategoryStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCategoryStatRepository extends JpaRepository<ReportCategoryStat, Long> {

    List<ReportCategoryStat> findByReportId(Long reportId);
}
