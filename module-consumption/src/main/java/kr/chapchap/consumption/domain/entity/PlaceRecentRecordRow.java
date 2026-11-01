package kr.chapchap.consumption.domain.entity;

import java.time.LocalDate;

public record PlaceRecentRecordRow(Long stickerItemId, Long imageId, String category, LocalDate receivedAt) {
}
