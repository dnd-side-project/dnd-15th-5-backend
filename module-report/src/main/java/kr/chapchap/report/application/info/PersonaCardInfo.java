package kr.chapchap.report.application.info;

import java.math.BigDecimal;
import java.util.List;

public record PersonaCardInfo(
        String nickname,
        String type,
        String typeName,
        String description,
        List<String> keywords,
        ScoresInfo scores
) {

    public record ScoresInfo(
            BigDecimal scoreExploration,
            BigDecimal scoreTownExpansion,
            BigDecimal scoreDaytime,
            BigDecimal scoreImpulsive
    ) {
    }
}
