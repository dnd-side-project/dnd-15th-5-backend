package kr.chapchap.report.domain.repository;

import kr.chapchap.report.domain.entity.ReportTimePattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportTimePatternRepository extends JpaRepository<ReportTimePattern, Long> {

    List<ReportTimePattern> findByReportId(Long reportId);
}
