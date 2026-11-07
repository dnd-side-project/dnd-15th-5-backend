package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.SummaryHighlightInfo;

public record SummaryHighlightResponse(String type, String mainCopy, String subCopy) {

    public static SummaryHighlightResponse from(SummaryHighlightInfo info) {
        return new SummaryHighlightResponse(
                info.type() != null ? info.type().name() : null,
                info.mainCopy(),
                info.subCopy());
    }
}
