package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionImageUploadCommand;
import kr.chapchap.consumption.application.info.ConsumptionImageUploadInfo;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsumptionImageUploadService {

    private static final Duration TEMPORARY_RETENTION = Duration.ofHours(24);

    private final ImageValidator imageValidator;
    private final ConsumptionImageStorage imageStorage;
    private final ConsumptionImageCommandService imageCommandService;
    private final Clock clock;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConsumptionImageUploadInfo upload(ConsumptionImageUploadCommand command) {
        String contentType = imageValidator.validateAndGetContentType(command.content());
        String objectKey = imageStorage.store(command.userId(), command.content(), contentType);
        try {
            ConsumptionImage image = imageCommandService.saveTemporary(
                    command.userId(),
                    objectKey,
                    contentType,
                    command.content().length,
                    LocalDateTime.now(clock).plus(TEMPORARY_RETENTION)
            );
            return new ConsumptionImageUploadInfo(image.getId(), image.getExpiresAt());
        } catch (RuntimeException exception) {
            try {
                imageStorage.delete(objectKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }
}
