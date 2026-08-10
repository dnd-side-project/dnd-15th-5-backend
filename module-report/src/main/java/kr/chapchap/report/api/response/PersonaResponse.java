package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.PersonaInfo;

public record PersonaResponse(
        String type,
        String typeName,
        String description,
        ScoresResponse scores
) {

    public static PersonaResponse from(PersonaInfo info) {
        return new PersonaResponse(
                info.type(),
                info.typeName(),
                info.description(),
                ScoresResponse.from(info.scores())
        );
    }
}
