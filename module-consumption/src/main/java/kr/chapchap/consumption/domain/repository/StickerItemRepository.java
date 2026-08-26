package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.StickerItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StickerItemRepository extends JpaRepository<StickerItem, Long> {

    List<StickerItem> findAllByCategoryIn(List<String> categories);

    Optional<StickerItem> findByCategoryAndName(String category, String name);
}
