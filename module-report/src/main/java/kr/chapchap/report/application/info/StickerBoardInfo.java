package kr.chapchap.report.application.info;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record StickerBoardInfo(YearMonth yearMonth, List<DayVisualsInfo> days) {

    public record DayVisualsInfo(LocalDate date, List<StickerBoardVisualInfo> visuals) {
    }

    public record StickerBoardVisualInfo(
            Long consumptionId,
            String category,
            Long stickerItemId,
            String stickerName,
            Long imageId,
            String imageUrl
    ) {
    }
}
