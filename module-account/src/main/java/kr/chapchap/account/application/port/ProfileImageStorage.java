package kr.chapchap.account.application.port;

public interface ProfileImageStorage {

    String createReadUrl(String objectKey);

    String store(
            Long userId,
            byte[] content,
            String contentType
    );

    void delete(String objectKey);
}
