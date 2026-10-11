package kr.chapchap.consumption.application.port;

public interface ConsumptionImageStorage {

    String createReadUrl(String objectKey);

    String store(
            Long userId,
            byte[] content,
            String contentType
    );

    void delete(String objectKey);

    void deleteAllByUserId(Long userId);
}
