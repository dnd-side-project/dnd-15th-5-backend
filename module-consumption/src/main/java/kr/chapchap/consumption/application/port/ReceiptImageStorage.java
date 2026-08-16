package kr.chapchap.consumption.application.port;

public interface ReceiptImageStorage {

    String store(
            Long userId,
            byte[] content,
            String contentType
    );

    void delete(String objectKey);
}
