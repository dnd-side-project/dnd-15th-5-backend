package kr.chapchap.report.application.info;

import java.util.List;

public record SummaryEvaluationResult(
        int totalEverCount,
        int recentThreeMonthsDistinctPlaceCount,
        List<SummaryCandidateInfo> candidates
) {
}
