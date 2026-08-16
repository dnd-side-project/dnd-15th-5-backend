package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.event.ReceiptImageCleanupEvent;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public List<Long> findCleanupCandidateIds(
            LocalDateTime expiredAt,
            long afterId,
            int batchSize
    ) {
        return receiptImageRepository.findCleanupCandidateIds(
                afterId,
                expiredAt,
                ReceiptImageStatus.TEMPORARY,
                ReceiptImageStatus.DELETING,
                PageRequest.of(0, batchSize)
        );
    }

    @Transactional
    public Optional<String> prepareForCleanup(Long receiptImageId, LocalDateTime cleanupAt) {
        return receiptImageRepository.findByIdForUpdate(receiptImageId)
                .filter(receiptImage -> !receiptImage.isAttached())
                .flatMap(receiptImage -> prepareForCleanup(receiptImage, cleanupAt));
    }

    @Transactional
    public boolean deletePreparedImage(Long receiptImageId) {
        return receiptImageRepository.deleteByIdAndStatus(
                receiptImageId,
                ReceiptImageStatus.DELETING
        ) > 0;
    }

    private Optional<String> prepareForCleanup(
            ReceiptImage receiptImage,
            LocalDateTime cleanupAt
    ) {
        if (receiptImage.getStatus() == ReceiptImageStatus.DELETING) {
            return Optional.of(receiptImage.getObjectKey());
        }
        if (receiptImage.getStatus() != ReceiptImageStatus.TEMPORARY
                || !receiptImage.isExpiredAt(cleanupAt)) {
            return Optional.empty();
        }

        receiptImage.markDeleting(cleanupAt);
        return Optional.of(receiptImage.getObjectKey());
    }
}
