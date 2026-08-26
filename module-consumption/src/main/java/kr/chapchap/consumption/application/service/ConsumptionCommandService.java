package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;
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

    private static final long SPECIAL_STICKER_VISIT_COUNT = 3L;
    private static final String COMMON_STICKER_CATEGORY = "공통";
    private static final String SPECIAL_STICKER_CATEGORY = "스페셜";
    private static final String CROWN_STICKER_NAME = "왕관";

    private final ConsumptionRepository consumptionRepository;
    private final StickerItemRepository stickerItemRepository;
    private final ReceiptImageRepository receiptImageRepository;
    private final Clock clock;

    @Transactional
    public ConsumptionCreateInfo create(ConsumptionCreateCommand command, Long placeId) {
        if (placeId == null || placeId <= 0) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }

        StickerItem stickerItem = selectStickerItem(command, placeId);

        Consumption consumption = consumptionRepository.save(Consumption.create(
                command.userId(),
                placeId,
                command.purchaseDate(),
                command.purchaseTime(),
                command.amount(),
                command.category(),
                stickerItem.getId()
        ));

        attachReceiptImageIfPresent(command, consumption.getId());

        return ConsumptionCreateInfo.of(consumption, stickerItem);
    }

    private StickerItem selectStickerItem(ConsumptionCreateCommand command, Long placeId) {
        long visitCount = consumptionRepository.countByUserIdAndPlaceId(command.userId(), placeId) + 1;
        if (visitCount % SPECIAL_STICKER_VISIT_COUNT == 0) {
            return stickerItemRepository.findByCategoryAndName(SPECIAL_STICKER_CATEGORY, CROWN_STICKER_NAME)
                    .orElseThrow(() -> new IllegalStateException("스페셜 왕관 스티커가 등록되어 있지 않습니다."));
        }

        List<StickerItem> stickerItems = stickerItemRepository.findAllByCategoryIn(
                List.of(command.category(), COMMON_STICKER_CATEGORY)
        );
        if (stickerItems.isEmpty()) {
            throw new IllegalStateException("선택 가능한 스티커가 등록되어 있지 않습니다.");
        }

        return stickerItems.get(ThreadLocalRandom.current().nextInt(stickerItems.size()));
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
