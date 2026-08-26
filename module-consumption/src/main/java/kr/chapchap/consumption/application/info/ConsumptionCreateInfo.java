package kr.chapchap.consumption.application.info;

import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.StickerItem;

public record ConsumptionCreateInfo(
        Long consumptionId,
        String stickerCategory,
        String stickerName
) {

    public static ConsumptionCreateInfo of(Consumption consumption, StickerItem stickerItem) {
        return new ConsumptionCreateInfo(
                consumption.getId(),
                stickerItem.getCategory(),
                stickerItem.getName()
        );
    }
}
