package kr.chapchap.report.application.port;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyStickerLookupPort {

    // 최신순으로 정렬된 스티커 이름 목록
    List<String> findRecentStickerNames(Long userId, LocalDate from, LocalDate toExclusive);
}
