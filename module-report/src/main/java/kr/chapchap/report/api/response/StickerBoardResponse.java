package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.StickerBoardInfo;
import kr.chapchap.report.application.info.StickerBoardInfo.DayVisualsInfo;
import kr.chapchap.report.application.info.StickerBoardInfo.StickerBoardVisualInfo;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record StickerBoardResponse(String yearMonth, List<DayVisualsResponse> days) {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static StickerBoardResponse from(StickerBoardInfo info) {
        return new StickerBoardResponse(
                info.yearMonth().format(YEAR_MONTH_FORMAT),
                info.days().stream().map(DayVisualsResponse::from).toList());
    }

    public record DayVisualsResponse(String date, List<VisualItemResponse> visuals) {

        public static DayVisualsResponse from(DayVisualsInfo info) {
            return new DayVisualsResponse(
                    info.date().format(DATE_FORMAT),
                    info.visuals().stream().map(VisualItemResponse::from).toList());
        }
    }

    public record VisualItemResponse(
            Long consumptionId, String category, Long stickerItemId, String stickerName, Long imageId, String imageUrl
    ) {

        public static VisualItemResponse from(StickerBoardVisualInfo info) {
            return new VisualItemResponse(
                    info.consumptionId(), info.category(), info.stickerItemId(), info.stickerName(),
                    info.imageId(), info.imageUrl());
        }
    }
}
