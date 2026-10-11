package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionSaveCommand;
import kr.chapchap.consumption.application.info.ConsumptionUpdateInfo;
import kr.chapchap.consumption.application.port.PlaceResolvePort;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ConsumptionUpdateService {

    private final ConsumptionRepository consumptionRepository;
    private final PlaceResolvePort placeResolvePort;
    private final ConsumptionCommandService consumptionCommandService;
    private final ConsumptionImageCleanupService consumptionImageCleanupService;
    private final ReceiptImageCleanupService receiptImageCleanupService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update(Long consumptionId, ConsumptionSaveCommand command) {
        Consumption consumption = consumptionRepository.findByIdAndUserId(consumptionId, command.userId())
                .orElseThrow(() -> new BusinessException(ConsumptionErrorCode.CONSUMPTION_NOT_FOUND));
        consumption.validateImageType(command.imageId());
        Long placeId = placeResolvePort.resolve(command.place());
        ConsumptionUpdateInfo updated = consumptionCommandService.update(consumptionId, command, placeId);
        if (updated.replacedImageId() != null) {
            consumptionImageCleanupService.cleanupImage(updated.replacedImageId());
        }
        updated.replacedReceiptImageIds().forEach(receiptImageCleanupService::cleanupImage);
    }
}
