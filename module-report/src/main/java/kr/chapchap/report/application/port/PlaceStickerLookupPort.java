package kr.chapchap.report.application.port;

import java.time.LocalDate;
import java.util.List;

public interface PlaceStickerLookupPort {

    List<String> findRecentStickerNames(Long userId, Long placeId, LocalDate from, LocalDate toExclusive, int limit);
}
