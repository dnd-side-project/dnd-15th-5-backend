package kr.chapchap.consumption.application.port;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;

public interface ReceiptOcrPort {

    ReceiptOcrDocument recognize(
            byte[] content,
            String contentType
    );
}
