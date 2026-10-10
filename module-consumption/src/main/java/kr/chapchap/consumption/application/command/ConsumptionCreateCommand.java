package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record ConsumptionCreateCommand(
        Long userId,
        Long receiptImageId,
        PlaceResolveCommand place,
        LocalDate purchaseDate,
        LocalTime purchaseTime,
        Long amount,
        String category,
        Long stickerItemId,
        Long imageId,
        String memo
) {

    private static final int MAX_MEMO_LENGTH = 1000;
    private static final Set<String> CATEGORIES = Set.of("카페", "운동", "편의점/마트", "취미/놀거리", "음식점", "미용/뷰티", "쇼핑", "기타");

    public ConsumptionCreateCommand {
        category = category == null ? null : category.trim();

        if (userId == null || userId <= 0
                || (receiptImageId != null && receiptImageId <= 0)
                || place == null
                || purchaseDate == null
                || purchaseTime == null
                || (amount != null && amount <= 0)
                || category == null || !CATEGORIES.contains(category)
                || (stickerItemId == null) == (imageId == null)
                || (stickerItemId != null && stickerItemId <= 0)
                || (imageId != null && imageId <= 0)
                || (memo != null && memo.length() > MAX_MEMO_LENGTH)) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }
    }
}
