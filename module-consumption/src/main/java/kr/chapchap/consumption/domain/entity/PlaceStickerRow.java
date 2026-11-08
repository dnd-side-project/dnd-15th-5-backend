package kr.chapchap.consumption.domain.entity;

import java.time.LocalDate;


public record PlaceStickerRow(Long consumptionId, Long stickerItemId, Long imageId, LocalDate receivedAt) {
}
