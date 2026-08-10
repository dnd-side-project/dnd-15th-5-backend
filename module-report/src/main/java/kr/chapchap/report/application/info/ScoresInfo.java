package kr.chapchap.report.application.info;

import java.math.BigDecimal;

public record ScoresInfo(
        BigDecimal scoreExploration,
        BigDecimal scoreTownExpansion,
        BigDecimal scoreDaytime,
        BigDecimal scoreImpulsive
) {
}
