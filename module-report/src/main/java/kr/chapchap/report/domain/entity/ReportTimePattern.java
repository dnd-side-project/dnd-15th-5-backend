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
@Table(name = "report_time_pattern")
public class ReportTimePattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    // 1:월 ~ 7:일요일
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    // 0 ~ 23시
    @Column(name = "visit_hour", nullable = false)
    private int visitHour;

    @Column(name = "visit_count", nullable = false)
    private int visitCount;

    @Builder
    private ReportTimePattern(Long reportId, int dayOfWeek, int visitHour, int visitCount) {
        this.reportId = reportId;
        this.dayOfWeek = dayOfWeek;
        this.visitHour = visitHour;
        this.visitCount = visitCount;
    }
}
