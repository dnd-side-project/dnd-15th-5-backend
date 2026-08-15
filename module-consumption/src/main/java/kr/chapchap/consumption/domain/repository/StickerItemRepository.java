package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.StickerItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StickerItemRepository extends JpaRepository<StickerItem, Long> {
}
