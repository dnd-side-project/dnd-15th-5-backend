package kr.chapchap.report.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "report_town_rank")
public class ReportTownRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "town_name", nullable = false, length = 100)
    private String townName;

    @Column(name = "visit_count", nullable = false)
    private int visitCount;

    @Builder
    private ReportTownRank(Long reportId, int rank, String townName, int visitCount) {
        this.reportId = reportId;
        this.rank = rank;
        this.townName = townName;
        this.visitCount = visitCount;
    }
}
