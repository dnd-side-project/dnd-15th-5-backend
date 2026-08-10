package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.ScoresInfo;

import java.math.BigDecimal;

public record ScoresResponse(
        BigDecimal scoreExploration,
        BigDecimal scoreTownExpansion,
        BigDecimal scoreDaytime,
        BigDecimal scoreImpulsive
) {

    public static ScoresResponse from(ScoresInfo info) {
        return new ScoresResponse(
                info.scoreExploration(),
                info.scoreTownExpansion(),
                info.scoreDaytime(),
                info.scoreImpulsive()
        );
    }
}
