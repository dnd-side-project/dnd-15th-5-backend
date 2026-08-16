package kr.chapchap.consumption.api.response;

import kr.chapchap.consumption.application.info.PlaceDetailInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StatsInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StickerCountInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StickerInfo;

import java.time.LocalDate;
import java.util.List;

public record PlaceDetailResponse(
        Long placeId,
        String placeName,
        String category,
        String address,
        boolean isRegular,
        StatsResponse stats,
        List<RecentStickerResponse> recentStickers,
        List<StickerSummaryResponse> stickerSummary
) {
    public static PlaceDetailResponse from(PlaceDetailInfo info) {
        return new PlaceDetailResponse(
                info.placeId(), info.placeName(), info.category(), info.address(), info.isRegular(),
                StatsResponse.from(info.stats()),
                info.recentStickers().stream().map(RecentStickerResponse::from).toList(),
                info.stickerSummary().stream().map(StickerSummaryResponse::from).toList()
        );
    }

    public record StatsResponse(LocalDate firstVisitedDate, int monthlyVisitCount, long totalVisitCount,
                                 String totalVisitComment) {
        public static StatsResponse from(StatsInfo info) {
            return new StatsResponse(info.firstVisitedDate(), info.monthlyVisitCount(), info.totalVisitCount(),
                    info.totalVisitComment());
        }
    }

    public record RecentStickerResponse(String itemName, LocalDate receivedAt) {
        public static RecentStickerResponse from(StickerInfo info) {
            return new RecentStickerResponse(info.itemName(), info.receivedAt());
        }
    }

    public record StickerSummaryResponse(String itemName, long count) {
        public static StickerSummaryResponse from(StickerCountInfo info) {
            return new StickerSummaryResponse(info.itemName(), info.count());
        }
    }
}
