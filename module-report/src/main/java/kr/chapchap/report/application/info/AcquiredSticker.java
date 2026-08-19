package kr.chapchap.report.application.info;

import java.time.LocalDate;

// MonthlyStickerLookupPort의 조회 결과 DTO
public record AcquiredSticker(String itemName, LocalDate acquiredDate) {
}
