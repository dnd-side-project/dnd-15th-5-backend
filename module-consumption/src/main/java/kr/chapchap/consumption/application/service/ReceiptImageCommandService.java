package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.event.ReceiptImageCleanupEvent;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class ReceiptImageCommandService {

    private final ReceiptImageRepository receiptImageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReceiptImage saveTemporary(
            Long userId,
            String objectKey,
            String contentType,
            long fileSizeBytes,
            LocalDateTime expiresAt
    ) {
        eventPublisher.publishEvent(new ReceiptImageCleanupEvent(objectKey));
        return receiptImageRepository.save(ReceiptImage.createTemporary(
                userId,
                objectKey,
                contentType,
                fileSizeBytes,
                expiresAt
        ));
    }
}
