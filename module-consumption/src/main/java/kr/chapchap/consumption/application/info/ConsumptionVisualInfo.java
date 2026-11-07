package kr.chapchap.consumption.application.info;

public record ConsumptionVisualInfo(
        Long stickerItemId,
        String stickerName,
        Long imageId,
        String imageUrl
) {

    public static ConsumptionVisualInfo ofSticker(Long stickerItemId, String stickerName) {
        return new ConsumptionVisualInfo(stickerItemId, stickerName, null, null);
    }

    public static ConsumptionVisualInfo ofImage(Long imageId, String imageUrl) {
        return new ConsumptionVisualInfo(null, null, imageId, imageUrl);
    }
}
