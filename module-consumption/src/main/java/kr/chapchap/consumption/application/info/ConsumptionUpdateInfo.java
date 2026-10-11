package kr.chapchap.consumption.application.info;

import java.util.List;

public record ConsumptionUpdateInfo(Long replacedImageId, List<Long> replacedReceiptImageIds) {
}
