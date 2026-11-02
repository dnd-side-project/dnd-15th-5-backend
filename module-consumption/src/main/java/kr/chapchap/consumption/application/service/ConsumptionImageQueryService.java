package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ConsumptionImageQueryService {

    private final ConsumptionImageRepository consumptionImageRepository;
    private final ConsumptionImageStorage consumptionImageStorage;

    public String getReadUrl(Long userId, Long imageId) {
        ConsumptionImage image = consumptionImageRepository.findByIdAndUserId(imageId, userId)
                .orElseThrow(() -> new BusinessException(ConsumptionErrorCode.IMAGE_NOT_FOUND));
        return consumptionImageStorage.createReadUrl(image.getObjectKey());
    }
}
