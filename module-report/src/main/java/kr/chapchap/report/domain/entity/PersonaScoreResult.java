package kr.chapchap.report.domain.entity;

import java.math.BigDecimal;

public record PersonaScoreResult(
        PersonaType personaType,
        BigDecimal scoreExploration,
        BigDecimal scoreTownExpansion,
        BigDecimal scoreDaytime,
        BigDecimal scoreImpulsive
) {
}
