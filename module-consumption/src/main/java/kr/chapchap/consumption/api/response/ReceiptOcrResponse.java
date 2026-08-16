package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "영수증 OCR 인식 결과")
public record ReceiptOcrResponse(
        @Schema(description = "임시 저장된 영수증 이미지 ID", example = "15")
        Long receiptImageId,

        @Schema(description = "인식된 상호명", example = "투썸플레이스 신논현점", nullable = true)
        String storeName,

        @Schema(description = "인식된 주소", example = "서울특별시 강남구 봉은사로 125 1층", nullable = true)
        String address,

        @Schema(description = "인식된 구매 날짜", example = "2026-07-25", nullable = true)
        LocalDate purchaseDate,

        @Schema(description = "인식된 구매 시간", example = "11:20:00", nullable = true)
        LocalTime purchaseTime,

        @Schema(description = "인식된 결제 금액(원)", example = "33000", nullable = true)
        Long amount
) {

    public static ReceiptOcrResponse from(ReceiptOcrInfo info) {
        return new ReceiptOcrResponse(
                info.receiptImageId(),
                info.storeName(),
                info.address(),
                info.purchaseDate(),
                info.purchaseTime(),
                info.amount()
        );
    }
}
