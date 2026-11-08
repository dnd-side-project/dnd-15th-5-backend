package kr.chapchap.report.application.info;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReportCalendarInfo(List<DayEntryInfo> days) {

    public record DayEntryInfo(LocalDate date, List<RecordDetailInfo> records) {
    }

    public record RecordDetailInfo(
            Long consumptionId,
            Long placeId,
            String placeName,
            LocalTime time,
            String category,
            Long amount,
            int visitOrdinal,
            Long stickerItemId,
            String stickerName,
            Long imageId,
            String imageUrl
    ) {
    }
}
