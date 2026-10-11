package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ConsumptionDeleteInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ConsumptionDeleteService {

    private final ConsumptionCommandService consumptionCommandService;
    private final ConsumptionImageCleanupService consumptionImageCleanupService;
    private final ReceiptImageCleanupService receiptImageCleanupService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void delete(Long userId, Long consumptionId) {
        ConsumptionDeleteInfo deleted = consumptionCommandService.delete(userId, consumptionId);
        // DB 삭제가 커밋된 뒤에만 S3 파일을 정리한다.
        if (deleted.imageId() != null) {
            consumptionImageCleanupService.cleanupImage(deleted.imageId());
        }
        deleted.receiptImageIds().forEach(receiptImageCleanupService::cleanupImage);
    }
}
