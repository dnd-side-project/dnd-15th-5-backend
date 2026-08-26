package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.PersonaCardInfo;

import java.math.BigDecimal;
import java.util.List;

public record PersonaCardResponse(
        String nickname,
        String type,
        String typeName,
        String description,
        List<String> keywords,
        ScoresResponse scores
) {

    public static PersonaCardResponse from(PersonaCardInfo info) {
        return new PersonaCardResponse(
                info.nickname(),
                info.type(),
                info.typeName(),
                info.description(),
                info.keywords(),
                ScoresResponse.from(info.scores())
        );
    }

    public record ScoresResponse(
            BigDecimal scoreExploration,
            BigDecimal scoreTownExpansion,
            BigDecimal scoreDaytime,
            BigDecimal scoreImpulsive
    ) {

        public static ScoresResponse from(PersonaCardInfo.ScoresInfo info) {
            return new ScoresResponse(
                    info.scoreExploration(),
                    info.scoreTownExpansion(),
                    info.scoreDaytime(),
                    info.scoreImpulsive()
            );
        }
    }
}
