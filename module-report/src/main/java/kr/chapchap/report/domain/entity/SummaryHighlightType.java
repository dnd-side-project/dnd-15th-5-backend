package kr.chapchap.report.domain.entity;


public enum SummaryHighlightType {
    // 당월 버킷
    EXPLORER(SummaryPeriodBucket.CURRENT_MONTH),           // 탐험형
    TASTE_FOCUSED(SummaryPeriodBucket.CURRENT_MONTH),      // 취향 집중형
    AREA_VISITOR(SummaryPeriodBucket.CURRENT_MONTH),       // 방문 지역형
    DIVERSE_TASTE(SummaryPeriodBucket.CURRENT_MONTH),      // 다양한 취향형

    // 최근 3개월 버킷
    LOYAL(SummaryPeriodBucket.RECENT_3_MONTHS),            // 단골형
    LOCAL_FOCUSED(SummaryPeriodBucket.RECENT_3_MONTHS),    // 동네 집중형
    CONSISTENT_RECORDER(SummaryPeriodBucket.RECENT_3_MONTHS), // 꾸준한 기록형

    // 전체 버킷
    FAVORITE_PLACE(SummaryPeriodBucket.ALL_TIME);          // 최애 가게형

    private final SummaryPeriodBucket periodBucket;

    SummaryHighlightType(SummaryPeriodBucket periodBucket) {
        this.periodBucket = periodBucket;
    }

    public SummaryPeriodBucket periodBucket() {
        return periodBucket;
    }
}
