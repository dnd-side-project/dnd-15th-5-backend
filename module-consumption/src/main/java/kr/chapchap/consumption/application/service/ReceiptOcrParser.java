package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CLOVA General OCR 결과에서 상호명, 주소, 거래 일시, 금액을 뽑는다.
 *
 * OCR 응답에 들어 있는 글자, 줄바꿈, 신뢰도, 좌표는 ReceiptOcrLayout에서 먼저 정리한다.
 * 정리한 결과는 항목별 추출기로 넘기며, 각 추출기는 서로 영향을 주지 않는다.
 * 예를 들어 주소를 못 찾더라도 날짜나 금액까지 버리지는 않는다.
 */
@Component
public class ReceiptOcrParser {

    private final ReceiptStoreExtractor storeExtractor = new ReceiptStoreExtractor();
    private final ReceiptAddressExtractor addressExtractor = new ReceiptAddressExtractor();
    private final ReceiptDateTimeExtractor dateTimeExtractor = new ReceiptDateTimeExtractor();
    private final ReceiptAmountExtractor amountExtractor = new ReceiptAmountExtractor();

    ParsedReceipt parse(ReceiptOcrDocument document) {
        ReceiptOcrLayout layout = ReceiptOcrLayout.from(document);
        ReceiptDateTimeExtractor.Result dateTime = dateTimeExtractor.extract(layout.lines());
        return new ParsedReceipt(
                storeExtractor.extract(layout),
                addressExtractor.extract(layout),
                dateTime.date(),
                dateTime.time(),
                amountExtractor.extract(layout)
        );
    }

    record ParsedReceipt(
            String storeName,
            String address,
            LocalDate purchaseDate,
            LocalTime purchaseTime,
            Long amount
    ) {
    }
}
