package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.StickerItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class StickerQueryService {

    private final StickerItemRepository stickerItemRepository;

    public Map<Long, String> findNames(List<Long> stickerItemIds) {
        List<Long> distinctIds = stickerItemIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }

        return stickerItemRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(StickerItem::getId, StickerItem::getName));
    }
}
