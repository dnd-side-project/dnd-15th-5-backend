package kr.chapchap.report.application.info;

import kr.chapchap.report.domain.entity.SummaryHighlightType;

public record SummaryHighlightInfo(SummaryHighlightType type, String mainCopy, String subCopy) {
}
