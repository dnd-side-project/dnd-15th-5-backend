package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ConsumptionVisualInfo;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ConsumptionVisualBatchResolver {

    private final ConsumptionRepository consumptionRepository;
    private final ConsumptionImageRepository consumptionImageRepository;
    private final ConsumptionImageStorage consumptionImageStorage;
    private final StickerQueryService stickerQueryService;


    public Map<Long, ConsumptionVisualInfo> resolveByConsumptionIds(Long userId, List<Long> consumptionIds) {
        List<Consumption> consumptions = consumptionRepository.findAllById(consumptionIds).stream()
                .filter(consumption -> consumption.getUserId().equals(userId))
                .toList();
        return resolveByConsumptions(userId, consumptions);
    }

    public Map<Long, ConsumptionVisualInfo> resolveByConsumptions(Long userId, List<Consumption> consumptions) {
        Map<Long, Long> stickerItemIdByConsumptionId = consumptions.stream()
                .filter(consumption -> consumption.getStickerItemId() != null)
                .collect(Collectors.toMap(Consumption::getId, Consumption::getStickerItemId));

        Map<Long, Long> imageIdByConsumptionId = consumptions.stream()
                .filter(consumption -> consumption.getImageId() != null)
                .collect(Collectors.toMap(Consumption::getId, Consumption::getImageId));

        Map<Long, StickerItem> stickerItemsById = stickerQueryService.findItems(
                List.copyOf(stickerItemIdByConsumptionId.values()));

        Map<Long, ConsumptionImage> imagesById = consumptionImageRepository
                .findAllByIdInAndUserId(List.copyOf(imageIdByConsumptionId.values()), userId).stream()
                .filter(ConsumptionImage::isAttached)
                .collect(Collectors.toMap(ConsumptionImage::getId, image -> image));

        Map<Long, ConsumptionVisualInfo> visualsByConsumptionId = new HashMap<>();

        stickerItemIdByConsumptionId.forEach((consumptionId, stickerItemId) -> {
            StickerItem stickerItem = stickerItemsById.get(stickerItemId);
            if (stickerItem != null) {
                visualsByConsumptionId.put(consumptionId,
                        ConsumptionVisualInfo.ofSticker(stickerItem.getId(), stickerItem.getName()));
            }
        });

        imageIdByConsumptionId.forEach((consumptionId, imageId) -> {
            ConsumptionImage image = imagesById.get(imageId);
            if (image != null) {
                String imageUrl = consumptionImageStorage.createReadUrl(image.getObjectKey());
                visualsByConsumptionId.put(consumptionId, ConsumptionVisualInfo.ofImage(imageId, imageUrl));
            }
        });

        return visualsByConsumptionId;
    }
}
