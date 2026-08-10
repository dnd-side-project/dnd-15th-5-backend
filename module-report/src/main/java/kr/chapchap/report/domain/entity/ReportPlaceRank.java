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

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "report_place_rank")
public class ReportPlaceRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "place_name", nullable = false, length = 150)
    private String placeName;

    @Column(name = "visit_count", nullable = false)
    private int visitCount;

    // 이 가게에 대한 Consumption.purchaseDate 중 가장 오래된 값 (배치가 채움). 처음 방문한 적이 없으면 null
    @Column(name = "first_visited_date")
    private LocalDate firstVisitedDate;

    @Builder
    private ReportPlaceRank(Long reportId, int rank, Long placeId, String placeName, int visitCount,
                             LocalDate firstVisitedDate) {
        this.reportId = reportId;
        this.rank = rank;
        this.placeId = placeId;
        this.placeName = placeName;
        this.visitCount = visitCount;
        this.firstVisitedDate = firstVisitedDate;
    }
}
