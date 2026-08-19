package kr.chapchap.report.application.port;

import kr.chapchap.report.application.info.AcquiredSticker;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyStickerLookupPort {
    List<AcquiredSticker> findRecentAcquiredStickers(Long userId, LocalDate from, LocalDate toExclusive);
}
