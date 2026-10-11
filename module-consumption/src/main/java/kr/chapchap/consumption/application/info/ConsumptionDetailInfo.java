package kr.chapchap.consumption.application.info;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConsumptionDetailInfo(
        Long consumptionId,
        Long placeId,
        String googlePlaceId,
        String placeName,
        String roadAddress,
        Double latitude,
        Double longitude,
        String thumbnailUrl,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        Long amount,
        String category,
        Long stickerItemId,
        String stickerCategory,
        String stickerName,
        Long imageId,
        String imageUrl,
        String memo
) {
}
