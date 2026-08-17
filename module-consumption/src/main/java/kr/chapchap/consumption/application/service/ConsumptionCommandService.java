package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.info.ConsumptionInfo;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.entity.ReceiptImageStatus;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import kr.chapchap.consumption.domain.repository.StickerItemRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
public class ConsumptionCommandService {

    private final ConsumptionRepository consumptionRepository;
    private final StickerItemRepository stickerItemRepository;
    private final ReceiptImageRepository receiptImageRepository;
    private final Clock clock;

    @Transactional
    public ConsumptionInfo create(ConsumptionCreateCommand command, Long placeId) {
        if (placeId == null || placeId <= 0) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }

        Long stickerItemId = selectRandomStickerItemId(command.category());

        Consumption consumption = consumptionRepository.save(Consumption.create(
                command.userId(),
                placeId,
                command.purchaseDate(),
                command.purchaseTime(),
                command.amount(),
                command.category(),
                stickerItemId
        ));

        attachReceiptImageIfPresent(command, consumption.getId());

        return ConsumptionInfo.of(consumption, command.place().placeName());
    }

    private Long selectRandomStickerItemId(String category) {
        List<StickerItem> stickerItems = stickerItemRepository.findAllByCategory(category);
        if (stickerItems.isEmpty()) {
            return null;
        }

        return stickerItems.get(ThreadLocalRandom.current().nextInt(stickerItems.size())).getId();
    }

    private void attachReceiptImageIfPresent(ConsumptionCreateCommand command, Long consumptionId) {
        if (command.receiptImageId() == null) {
            return;
        }

        ReceiptImage receiptImage = receiptImageRepository.findByIdAndUserIdForUpdate(
                        command.receiptImageId(),
                        command.userId()
                )
                .orElseThrow(() -> new BusinessException(ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND));

        if (receiptImage.isAttached()) {
            throw new BusinessException(ConsumptionErrorCode.RECEIPT_IMAGE_ALREADY_ATTACHED);
        }

        LocalDateTime attachedAt = LocalDateTime.now(clock);
        if (receiptImage.getStatus() != ReceiptImageStatus.TEMPORARY
                || receiptImage.isExpiredAt(attachedAt)) {
            throw new BusinessException(ConsumptionErrorCode.RECEIPT_IMAGE_EXPIRED);
        }

        receiptImage.attach(consumptionId, attachedAt);
    }
}
