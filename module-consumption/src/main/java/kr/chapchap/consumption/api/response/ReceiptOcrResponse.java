package kr.chapchap.consumption.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;
import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;

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
        Long amount,

        @Schema(description = "OCR 결과로 조회한 Google Place 검색 결과", nullable = true)
        GooglePlaceSearchResultResponse googlePlaceSearchResult
) {

    public static ReceiptOcrResponse from(ReceiptOcrInfo info) {
        GooglePlaceSearchResultInfo googlePlaceSearchResult = info.googlePlaceSearchResult();
        return new ReceiptOcrResponse(
                info.receiptImageId(),
                info.storeName(),
                info.address(),
                info.purchaseDate(),
                info.purchaseTime(),
                info.amount(),
                googlePlaceSearchResult != null
                        ? new GooglePlaceSearchResultResponse(
                                googlePlaceSearchResult.googlePlaceId(),
                                googlePlaceSearchResult.placeName(),
                                googlePlaceSearchResult.roadAddress(),
                                googlePlaceSearchResult.latitude(),
                                googlePlaceSearchResult.longitude(),
                                googlePlaceSearchResult.thumbnailUrl()
                        )
                        : null
        );
    }

    @Schema(description = "Google Place 검색 결과")
    public record GooglePlaceSearchResultResponse(
            @Schema(description = "Google Place ID", example = "ChIJ123")
            String googlePlaceId,

            @Schema(description = "장소명", example = "투썸플레이스 신논현점")
            String placeName,

            @Schema(description = "도로명주소", example = "서울특별시 강남구 봉은사로 125 1층")
            String roadAddress,

            @Schema(description = "위도", example = "37.5065")
            double latitude,

            @Schema(description = "경도", example = "127.0241")
            double longitude,

            @Schema(description = "장소 썸네일 URL", example = "https://lh3.googleusercontent.com/photo", nullable = true)
            String thumbnailUrl
    ) {
    }
}
