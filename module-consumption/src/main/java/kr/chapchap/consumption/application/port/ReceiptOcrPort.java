package kr.chapchap.consumption.application.port;

import java.util.List;

public interface ReceiptOcrPort {

    List<String> recognize(
            byte[] content,
            String contentType
    );
}
