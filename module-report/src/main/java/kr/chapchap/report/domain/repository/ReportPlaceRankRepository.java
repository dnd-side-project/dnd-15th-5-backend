package kr.chapchap.report.domain.repository;

import kr.chapchap.report.domain.entity.ReportPlaceRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportPlaceRankRepository extends JpaRepository<ReportPlaceRank, Long> {

    List<ReportPlaceRank> findByReportIdOrderByRankAsc(Long reportId);
}
