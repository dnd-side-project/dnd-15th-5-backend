package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.ConsumptionImageStatus;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ConsumptionImageCommandService {

    private final ConsumptionImageRepository consumptionImageRepository;

    @Transactional
    public ConsumptionImage saveTemporary(
            Long userId,
            String objectKey,
            String contentType,
            long fileSizeBytes,
            LocalDateTime expiresAt
    ) {
        return consumptionImageRepository.save(ConsumptionImage.createTemporary(
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
        return consumptionImageRepository.findCleanupCandidateIds(
                afterId,
                expiredAt,
                ConsumptionImageStatus.TEMPORARY,
                ConsumptionImageStatus.DELETING,
                PageRequest.of(0, batchSize)
        );
    }

    @Transactional
    public Optional<String> prepareForCleanup(Long consumptionImageId, LocalDateTime cleanupAt) {
        return consumptionImageRepository.findByIdForUpdate(consumptionImageId)
                .filter(consumptionImage -> !consumptionImage.isAttached())
                .flatMap(consumptionImage -> prepareForCleanup(consumptionImage, cleanupAt));
    }

    @Transactional
    public boolean deletePreparedImage(Long consumptionImageId) {
        return consumptionImageRepository.deleteByIdAndStatus(
                consumptionImageId,
                ConsumptionImageStatus.DELETING
        ) > 0;
    }

    private Optional<String> prepareForCleanup(
            ConsumptionImage consumptionImage,
            LocalDateTime cleanupAt
    ) {
        if (consumptionImage.getStatus() == ConsumptionImageStatus.DELETING) {
            return Optional.of(consumptionImage.getObjectKey());
        }
        if (consumptionImage.getStatus() != ConsumptionImageStatus.TEMPORARY
                || !consumptionImage.isExpiredAt(cleanupAt)) {
            return Optional.empty();
        }

        consumptionImage.markDeleting(cleanupAt);
        return Optional.of(consumptionImage.getObjectKey());
    }
}
