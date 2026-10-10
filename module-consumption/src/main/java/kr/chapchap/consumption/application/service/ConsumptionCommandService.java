package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.ConsumptionImageStatus;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
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

@RequiredArgsConstructor
@Service
public class ConsumptionCommandService {

    private final ConsumptionRepository consumptionRepository;
    private final StickerItemRepository stickerItemRepository;
    private final ReceiptImageRepository receiptImageRepository;
    private final ConsumptionImageRepository consumptionImageRepository;
    private final Clock clock;

    @Transactional
    public ConsumptionCreateInfo create(ConsumptionCreateCommand command, Long placeId) {
        if (placeId == null || placeId <= 0) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }

        StickerItem stickerItem = selectStickerItem(command);

        attachImageIfPresent(command);

        Consumption consumption = consumptionRepository.save(Consumption.create(
                command.userId(),
                placeId,
                command.purchaseDate(),
                command.purchaseTime(),
                command.amount(),
                command.category(),
                command.stickerItemId(),
                command.imageId(),
                command.memo()
        ));

        attachReceiptImageIfPresent(command, consumption.getId());

        return ConsumptionCreateInfo.of(consumption, stickerItem);
    }

    private StickerItem selectStickerItem(ConsumptionCreateCommand command) {
        if (command.stickerItemId() == null) {
            return null;
        }
        StickerItem sticker = stickerItemRepository.findById(command.stickerItemId())
                .orElseThrow(() -> new BusinessException(ConsumptionErrorCode.STICKER_NOT_FOUND));
        if (!sticker.getCategory().equals(command.category())) {
            throw new BusinessException(ConsumptionErrorCode.STICKER_CATEGORY_MISMATCH);
        }
        return sticker;
    }

    private void attachImageIfPresent(ConsumptionCreateCommand command) {
        if (command.imageId() == null) {
            return;
        }
        ConsumptionImage image = consumptionImageRepository.findByIdAndUserIdForUpdate(
                command.imageId(), command.userId()
        ).orElseThrow(() -> new BusinessException(ConsumptionErrorCode.IMAGE_NOT_FOUND));
        if (image.isAttached()) {
            throw new BusinessException(ConsumptionErrorCode.IMAGE_ALREADY_ATTACHED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (image.getStatus() != ConsumptionImageStatus.TEMPORARY || image.isExpiredAt(now)) {
            throw new BusinessException(ConsumptionErrorCode.IMAGE_EXPIRED);
        }
        image.attach(now);
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
