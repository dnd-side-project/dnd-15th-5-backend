package kr.chapchap.report.domain.repository;

import kr.chapchap.report.domain.entity.ReportTownRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportTownRankRepository extends JpaRepository<ReportTownRank, Long> {

    List<ReportTownRank> findByReportIdOrderByRankAsc(Long reportId);
}
