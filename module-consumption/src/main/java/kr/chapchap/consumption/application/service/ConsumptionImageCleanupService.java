package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
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
public class ConsumptionImageCleanupService {

    private static final int CLEANUP_BATCH_SIZE = 100;

    private final ConsumptionImageCommandService consumptionImageCommandService;
    private final ConsumptionImageStorage consumptionImageStorage;
    private final Clock clock;

    public void deleteAllByUserId(Long userId) {
        consumptionImageStorage.deleteAllByUserId(userId);
    }

    public int cleanupExpiredImages() {
        LocalDateTime cleanupAt = LocalDateTime.now(clock);
        long afterId = 0L;
        int deletedCount = 0;
        int failedCount = 0;

        while (true) {
            List<Long> candidateIds = consumptionImageCommandService.findCleanupCandidateIds(
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
                            "만료된 소비기록 이미지 정리에 실패했습니다. consumptionImageId={}",
                            candidateId,
                            exception
                    );
                }
            }
            afterId = candidateIds.getLast();
        }

        log.info(
                "만료된 소비기록 이미지 정리를 완료했습니다. deletedCount={}, failedCount={}",
                deletedCount,
                failedCount
        );
        return deletedCount;
    }

    private boolean cleanup(Long consumptionImageId, LocalDateTime cleanupAt) {
        Optional<String> objectKey = consumptionImageCommandService.prepareForCleanup(
                consumptionImageId,
                cleanupAt
        );
        if (objectKey.isEmpty()) {
            return false;
        }

        consumptionImageStorage.delete(objectKey.get());
        return consumptionImageCommandService.deletePreparedImage(consumptionImageId);
    }
}
