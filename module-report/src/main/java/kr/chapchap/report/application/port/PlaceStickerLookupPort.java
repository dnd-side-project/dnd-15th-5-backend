package kr.chapchap.report.application.port;

import kr.chapchap.report.application.info.ConsumptionVisualSummary;

import java.time.LocalDate;
import java.util.List;

public interface PlaceStickerLookupPort {

    // 스티커 또는 유저 업로드 사진(최신순) - 1위 가게 카드용. limit null이면 전체(더보기로 펼칠 수 있어야 해서 서버는 안 자름)
    List<ConsumptionVisualSummary> findRecentVisuals(Long userId, Long placeId, LocalDate from, LocalDate toExclusive, Integer limit);
}
