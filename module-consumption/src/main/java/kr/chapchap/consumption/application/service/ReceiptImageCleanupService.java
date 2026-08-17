package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReceiptImageCleanupService {

    private static final int CLEANUP_BATCH_SIZE = 100;

    private final ReceiptImageCommandService receiptImageCommandService;
    private final ReceiptImageStorage receiptImageStorage;
    private final Clock clock;

    public int cleanupExpiredImages() {
        LocalDateTime cleanupAt = LocalDateTime.now(clock);
        long afterId = 0L;
        int deletedCount = 0;
        int failedCount = 0;

        while (true) {
            List<Long> candidateIds = receiptImageCommandService.findCleanupCandidateIds(
                    cleanupAt,
                    afterId,
                    CLEANUP_BATCH_SIZE
            );
            if (candidateIds.isEmpty()) {
                break;
            }

            for (Long candidateId : candidateIds) {
                try {
                    if (cleanup(candidateId, cleanupAt)) {
                        deletedCount++;
                    }
                } catch (RuntimeException exception) {
                    failedCount++;
                    log.error(
                            "만료된 영수증 이미지 정리에 실패했습니다. receiptImageId={}",
                            candidateId,
                            exception
                    );
                }
            }
            afterId = candidateIds.getLast();
        }

        log.info(
                "만료된 영수증 이미지 정리를 완료했습니다. deletedCount={}, failedCount={}",
                deletedCount,
                failedCount
        );
        return deletedCount;
    }

    private boolean cleanup(Long receiptImageId, LocalDateTime cleanupAt) {
        Optional<String> objectKey = receiptImageCommandService.prepareForCleanup(
                receiptImageId,
                cleanupAt
        );
        if (objectKey.isEmpty()) {
            return false;
        }

        receiptImageStorage.delete(objectKey.get());
        return receiptImageCommandService.deletePreparedImage(receiptImageId);
    }
}
