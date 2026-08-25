package kr.chapchap.report.api.response;

import kr.chapchap.report.application.info.AcquiredSticker;
import kr.chapchap.report.application.info.CurrentStatusInfo;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public record CurrentStatusResponse(
        String date,
        List<Integer> weeklyCounts,
        int monthlyCount,
        Map<String, Integer> monthlyCategoryCounts,
        String recentDiscoveryMessage,
        List<StickerResponse> monthlyStickers
) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static CurrentStatusResponse from(CurrentStatusInfo info) {
        return new CurrentStatusResponse(
                info.date().format(DATE_FORMAT),
                info.weeklyCounts(),
                info.monthlyCount(),
                info.monthlyCategoryCounts(),
                info.recentDiscoveryMessage(),
                info.monthlyStickers().stream().map(StickerResponse::from).toList()
        );
    }

    public record StickerResponse(String itemName, String acquiredDate) {

        private static final DateTimeFormatter ACQUIRED_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

        public static StickerResponse from(AcquiredSticker sticker) {
            return new StickerResponse(sticker.itemName(), sticker.acquiredDate().format(ACQUIRED_DATE_FORMAT));
        }
    }
}
