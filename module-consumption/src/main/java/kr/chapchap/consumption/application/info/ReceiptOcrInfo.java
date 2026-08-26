package kr.chapchap.consumption.application.info;

import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReceiptOcrInfo(
        Long receiptImageId,
        String storeName,
        String address,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        Long amount,
        GooglePlaceSearchResultInfo googlePlaceSearchResult
) {
}
