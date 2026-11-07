package kr.chapchap.report.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "report_summary_exposure")
public class ReportSummaryExposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exposure_date", nullable = false)
    private LocalDate exposureDate;

    @Column(name = "selected_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SummaryHighlightType selectedType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ReportSummaryExposure(Long userId, LocalDate exposureDate, SummaryHighlightType selectedType) {
        this.userId = userId;
        this.exposureDate = exposureDate;
        this.selectedType = selectedType;
    }

    public static ReportSummaryExposure create(Long userId, LocalDate exposureDate, SummaryHighlightType selectedType) {
        return ReportSummaryExposure.builder()
                .userId(userId)
                .exposureDate(exposureDate)
                .selectedType(selectedType)
                .build();
    }
}
