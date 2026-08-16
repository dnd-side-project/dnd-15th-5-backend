package kr.chapchap.consumption.domain.repository;

import kr.chapchap.consumption.domain.entity.StickerItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StickerItemRepository extends JpaRepository<StickerItem, Long> {

    List<StickerItem> findAllByCategory(String category);
}
