package kr.chapchap.consumption.application.info;

import java.util.List;

public record ConsumptionDeleteInfo(Long imageId, List<Long> receiptImageIds) {
}
