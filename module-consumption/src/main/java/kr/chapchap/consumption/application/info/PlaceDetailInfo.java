package kr.chapchap.consumption.application.info;

import java.time.LocalDate;
import java.util.List;

public record PlaceDetailInfo(
        Long placeId,
        String placeName,
        String category,
        String address,
        boolean isRegular,
        StatsInfo stats,
        List<StickerInfo> recentStickers,
        List<StickerCountInfo> stickerSummary
) {

    public record StatsInfo(LocalDate firstVisitedDate, int monthlyVisitCount, long totalVisitCount,
                             String totalVisitComment) {
    }

    public record StickerInfo(String itemName, LocalDate receivedAt) {
    }

    public record StickerCountInfo(String itemName, long count) {
    }
}
