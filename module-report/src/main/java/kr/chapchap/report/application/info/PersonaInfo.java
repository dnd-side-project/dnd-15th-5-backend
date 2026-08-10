package kr.chapchap.report.application.info;

public record PersonaInfo(
        String type,
        String typeName,
        String description,
        ScoresInfo scores
) {
}
