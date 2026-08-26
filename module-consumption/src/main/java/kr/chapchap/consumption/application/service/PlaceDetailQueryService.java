package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.PlaceVisitSearchCommand;
import kr.chapchap.consumption.application.info.PlaceDetailInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StatsInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StickerCountInfo;
import kr.chapchap.consumption.application.info.PlaceDetailInfo.StickerInfo;
import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.PlaceVisitScrollInfo.VisitInfo;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceStickerRow;
import kr.chapchap.consumption.domain.entity.PlaceVisitStatsRow;
import kr.chapchap.consumption.domain.entity.StickerCountRow;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.domain.service.PlaceVisitCommentGenerator;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PlaceDetailQueryService {

    private static final int TOP_REGULAR_RANK_SIZE = 3;
    private static final int MIN_VISIT_COUNT_FOR_REGULAR = 5;
    private static final int RECENT_STICKER_LIMIT = 5;
    private static final PlaceSummaryInfo UNKNOWN_PLACE_SUMMARY = new PlaceSummaryInfo("알 수 없는 가게", null, "주소 정보 없음", null, null);

    private final ConsumptionQueryRepository consumptionQueryRepository;
    private final StickerQueryService stickerQueryService;
    private final PlaceSummaryLookupPort placeSummaryLookupPort;
    private final PlaceVisitCommentGenerator placeVisitCommentGenerator;
    private final Clock clock;

    // 장소 상세 (헤더)
    public PlaceDetailInfo getDetail(Long userId, Long placeId) {
        PlaceVisitStatsRow stats = consumptionQueryRepository.findVisitStats(userId, placeId)
                .orElseThrow(() -> new BusinessException(ConsumptionErrorCode.PLACE_VISIT_NOT_FOUND));

        YearMonth currentMonth = YearMonth.now(clock);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEndExclusive = currentMonth.plusMonths(1).atDay(1);
        long monthlyVisitCount = consumptionQueryRepository.countVisits(userId, placeId, monthStart, monthEndExclusive);

        boolean isRegular = isRegular(userId, placeId);

        PlaceSummaryInfo summary = placeSummaryLookupPort.findSummaries(List.of(placeId)).getOrDefault(placeId, UNKNOWN_PLACE_SUMMARY);

        StatsInfo statsInfo = new StatsInfo(
                stats.firstVisitedDate(),
                (int) monthlyVisitCount,
                stats.totalVisitCount(),
                placeVisitCommentGenerator.generate(stats.totalVisitCount().intValue())
        );

        return new PlaceDetailInfo(
                placeId,
                summary.name(),
                stats.category(),
                summary.address(),
                isRegular,
                statsInfo,
                resolveRecentStickers(userId, placeId),
                resolveStickerSummary(userId, placeId)
        );
    }

    // 장소 상세 - 방문 이력 무한스크롤
    public PlaceVisitScrollInfo getVisits(PlaceVisitSearchCommand command) {
        int fetchSize = command.size() + 1;
        List<Consumption> fetched = consumptionQueryRepository.searchPlaceVisitsByCursor(
                command.userId(), command.placeId(), command.cursorPurchaseDate(), command.cursorPurchaseTime(),
                command.cursorId(), fetchSize
        );

        boolean hasNext = fetched.size() > command.size();
        List<Consumption> content = hasNext ? fetched.subList(0, command.size()) : fetched;

        List<VisitInfo> visits = content.stream()
                .map(consumption -> new VisitInfo(consumption.getPurchaseDate(), consumption.getAmount()))
                .toList();

        Consumption last = content.isEmpty() ? null : content.get(content.size() - 1);

        return new PlaceVisitScrollInfo(
                visits,
                hasNext,
                last != null ? last.getPurchaseDate() : null,
                last != null ? last.getPurchaseTime() : null,
                last != null ? last.getId() : null
        );
    }


    private boolean isRegular(Long userId, Long placeId) {
        List<PlaceCategoryVisitRow> top = consumptionQueryRepository.aggregatePlaceRankingByCursor(
                userId, null, null, null, null, null, TOP_REGULAR_RANK_SIZE
        );
        return top.stream()
                .anyMatch(row -> row.placeId().equals(placeId) && row.visitCount() >= MIN_VISIT_COUNT_FOR_REGULAR);
    }

    private List<StickerInfo> resolveRecentStickers(Long userId, Long placeId) {
        List<PlaceStickerRow> rows = consumptionQueryRepository.findRecentStickersByPlace(
                userId, placeId, null, null, RECENT_STICKER_LIMIT);
        Map<Long, StickerItem> stickerItems = stickerQueryService.findItems(
                rows.stream().map(PlaceStickerRow::stickerItemId).toList());

        return rows.stream()
                .map(row -> {
                    StickerItem stickerItem = stickerItems.get(row.stickerItemId());
                    return new StickerInfo(stickerItem.getCategory(), stickerItem.getName(), row.receivedAt());
                })
                .toList();
    }

    private List<StickerCountInfo> resolveStickerSummary(Long userId, Long placeId) {
        List<StickerCountRow> rows = consumptionQueryRepository.aggregateStickerCountsByPlace(userId, placeId);
        Map<Long, StickerItem> stickerItems = stickerQueryService.findItems(
                rows.stream().map(StickerCountRow::stickerItemId).toList());

        return rows.stream()
                .map(row -> {
                    StickerItem stickerItem = stickerItems.get(row.stickerItemId());
                    return new StickerCountInfo(stickerItem.getCategory(), stickerItem.getName(), row.count());
                })
                .toList();
    }
}
