package kr.chapchap.consumption.application.info;

import kr.chapchap.consumption.domain.entity.StickerItem;

public record StickerItemInfo(Long stickerItemId, String category, String name) {
    public static StickerItemInfo from(StickerItem sticker) {
        return new StickerItemInfo(sticker.getId(), sticker.getCategory(), sticker.getName());
    }
}
