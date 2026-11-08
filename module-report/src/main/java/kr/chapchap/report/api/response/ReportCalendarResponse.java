package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.ReportCalendarInfo;
import kr.chapchap.report.application.info.ReportCalendarInfo.DayEntryInfo;
import kr.chapchap.report.application.info.ReportCalendarInfo.RecordDetailInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ReportCalendarResponse(List<DayEntryResponse> days) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static ReportCalendarResponse from(ReportCalendarInfo info) {
        return new ReportCalendarResponse(info.days().stream().map(DayEntryResponse::from).toList());
    }

    public record DayEntryResponse(String date, List<RecordDetailResponse> records) {

        public static DayEntryResponse from(DayEntryInfo info) {
            return new DayEntryResponse(
                    info.date().format(DATE_FORMAT),
                    info.records().stream().map(RecordDetailResponse::from).toList());
        }
    }

    public record RecordDetailResponse(
            Long consumptionId,
            Long placeId,
            String placeName,
            String time,
            String category,
            Long amount,
            int visitOrdinal,
            Long stickerItemId,
            String stickerName,
            Long imageId,
            String imageUrl
    ) {

        public static RecordDetailResponse from(RecordDetailInfo info) {
            return new RecordDetailResponse(
                    info.consumptionId(),
                    info.placeId(),
                    info.placeName(),
                    formatTime(info.time()),
                    info.category(),
                    info.amount(),
                    info.visitOrdinal(),
                    info.stickerItemId(),
                    info.stickerName(),
                    info.imageId(),
                    info.imageUrl()
            );
        }

        private static String formatTime(LocalTime time) {
            return time != null ? time.format(TIME_FORMAT) : null;
        }
    }
}
