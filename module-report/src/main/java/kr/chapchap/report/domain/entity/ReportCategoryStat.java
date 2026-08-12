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

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "report_category_stat")
public class ReportCategoryStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "percentage", nullable = false)
    private BigDecimal percentage;

    @Builder
    private ReportCategoryStat(Long reportId, String category, BigDecimal percentage) {
        this.reportId = reportId;
        this.category = category;
        this.percentage = percentage;
    }
}
