package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.application.info.StickerItemInfo;
import org.springframework.data.domain.Sort;
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

    public List<StickerItemInfo> findAll() {
        return stickerItemRepository.findAll(Sort.by("category", "id")).stream()
                .map(StickerItemInfo::from)
                .toList();
    }

    public Map<Long, StickerItem> findItems(List<Long> stickerItemIds) {
        List<Long> distinctIds = stickerItemIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }

        return stickerItemRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(StickerItem::getId, stickerItem -> stickerItem));
    }

    public Map<Long, String> findNames(List<Long> stickerItemIds) {
        return findItems(stickerItemIds).values().stream()
                .collect(Collectors.toMap(StickerItem::getId, StickerItem::getName));
    }
}
