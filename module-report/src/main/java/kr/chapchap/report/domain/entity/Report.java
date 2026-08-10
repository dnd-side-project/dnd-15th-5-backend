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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "persona_type", nullable = false, length = 50)
    private PersonaType personaType;

    @Column(name = "score_exploration")
    private BigDecimal scoreExploration;

    @Column(name = "score_town_expansion")
    private BigDecimal scoreTownExpansion;

    @Column(name = "score_daytime")
    private BigDecimal scoreDaytime;

    @Column(name = "score_impulsive")
    private BigDecimal scoreImpulsive;

    @Column(name = "total_visit_count", nullable = false)
    private int totalVisitCount;

    @Column(name = "new_town_count", nullable = false)
    private int newTownCount;

    @Column(name = "new_place_count", nullable = false)
    private int newPlaceCount;

    @Column(name = "new_sticker_count", nullable = false)
    private int newStickerCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Report(Long userId, LocalDate reportMonth, PersonaType personaType,
                    BigDecimal scoreExploration, BigDecimal scoreTownExpansion,
                    BigDecimal scoreDaytime, BigDecimal scoreImpulsive,
                    int totalVisitCount, int newTownCount, int newPlaceCount, int newStickerCount) {
        this.userId = userId;
        this.reportMonth = reportMonth;
        this.personaType = personaType;
        this.scoreExploration = scoreExploration;
        this.scoreTownExpansion = scoreTownExpansion;
        this.scoreDaytime = scoreDaytime;
        this.scoreImpulsive = scoreImpulsive;
        this.totalVisitCount = totalVisitCount;
        this.newTownCount = newTownCount;
        this.newPlaceCount = newPlaceCount;
        this.newStickerCount = newStickerCount;
    }
}
