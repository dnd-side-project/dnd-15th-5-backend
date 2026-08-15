package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.port.PlaceLikeLookupPort;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceFirstStickerRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class VisitedPlaceQueryService {

    private static final String UNKNOWN_PLACE_NAME = "알 수 없는 가게";

    private final ConsumptionQueryRepository consumptionQueryRepository; //소비 집계
    private final PlaceSummaryLookupPort placeSummaryLookupPort; //가게 이름/위치 등 조회
    private final PlaceLikeLookupPort placeLikeLookupPort; //좋아요 여부 조회
    private final StickerQueryService stickerQueryService; //스티커 이름 조회
    private final Clock clock;

    public VisitedPlaceMarkersInfo getVisitedPlaceMarkers(Long userId, List<String> categories) {
        YearMonth currentMonth = YearMonth.now(clock);
        int monthlyPlaceCount = calculateMonthlyPlaceCount(userId, currentMonth);

        //방문 한 곳
        List<PlaceCategoryVisitRow> rows = consumptionQueryRepository.aggregateVisitedPlacesByCategory(userId, categories);
        List<Long> visitedPlaceIds = rows.stream().map(PlaceCategoryVisitRow::placeId).toList();
        Set<Long> visitedPlaceIdSet = Set.copyOf(visitedPlaceIds);

        Set<Long> likedPlaceIds = placeLikeLookupPort.findLikedPlaceIds(userId); //좋아요
        List<Long> likedOnlyPlaceIds = likedPlaceIds.stream().filter(placeId -> !visitedPlaceIdSet.contains(placeId)).toList(); //방문x 좋아요

        if (rows.isEmpty() && likedOnlyPlaceIds.isEmpty()) {
            return new VisitedPlaceMarkersInfo(List.of(), currentMonth.getMonthValue(), monthlyPlaceCount);
        }

        List<Long> allPlaceIds = Stream.concat(visitedPlaceIds.stream(), likedOnlyPlaceIds.stream()).toList();
        Map<Long, PlaceSummaryInfo> summaries = placeSummaryLookupPort.findSummaries(allPlaceIds);
        requireAllLocationsPresent(allPlaceIds, summaries);
        Map<Long, String> firstStickerNameByPlace = findFirstStickerNamesByPlace(userId, visitedPlaceIds);

        List<VisitedPlaceMarkerInfo> visitedMarkers = rows.stream()
                .map(row -> toMarkerInfo(row, summaries, likedPlaceIds, firstStickerNameByPlace))
                .toList();

        // 방문X 좋아요
        List<VisitedPlaceMarkerInfo> likedOnlyMarkers = likedOnlyPlaceIds.stream()
                .map(placeId -> toLikedOnlyMarkerInfo(placeId, summaries))
                .toList();

        List<VisitedPlaceMarkerInfo> markers = Stream.concat(visitedMarkers.stream(), likedOnlyMarkers.stream())
                .sorted(Comparator.comparing(VisitedPlaceMarkerInfo::visitCount).reversed())
                .toList();

        return new VisitedPlaceMarkersInfo(markers, currentMonth.getMonthValue(), monthlyPlaceCount);
    }

    private int calculateMonthlyPlaceCount(Long userId, YearMonth currentMonth) {
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEndExclusive = currentMonth.plusMonths(1).atDay(1);
        return (int) consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(userId, monthStart, monthEndExclusive);
    }


    private Map<Long, String> findFirstStickerNamesByPlace(Long userId, List<Long> visitedPlaceIds) {
        if (visitedPlaceIds.isEmpty()) {
            return Map.of();
        }

        List<PlaceFirstStickerRow> firstStickerRows = consumptionQueryRepository.findFirstStickerItemIdsByPlace(userId, visitedPlaceIds);
        if (firstStickerRows.isEmpty()) {
            return Map.of();
        }

        List<Long> stickerItemIds = firstStickerRows.stream().map(PlaceFirstStickerRow::stickerItemId).toList();
        Map<Long, String> namesById = stickerQueryService.findNames(stickerItemIds);

        return firstStickerRows.stream()
                .filter(row -> namesById.containsKey(row.stickerItemId()))
                .collect(Collectors.toMap(PlaceFirstStickerRow::placeId,
                        row -> namesById.get(row.stickerItemId())));
    }

    private void requireAllLocationsPresent(List<Long> placeIds, Map<Long, PlaceSummaryInfo> summaries) {
        boolean anyMissing = placeIds.stream().distinct()
                .anyMatch(placeId -> summaries.get(placeId) == null || summaries.get(placeId).latitude() == null);
        if (anyMissing) {
            throw new BusinessException(ConsumptionErrorCode.PLACE_LOCATION_NOT_FOUND);
        }
    }

    private VisitedPlaceMarkerInfo toMarkerInfo(PlaceCategoryVisitRow row, Map<Long, PlaceSummaryInfo> summaries,
                                                 Set<Long> likedPlaceIds, Map<Long, String> firstStickerNameByPlace) {
        PlaceSummaryInfo summary = summaries.get(row.placeId());

        return new VisitedPlaceMarkerInfo(
                row.placeId(),
                summary.name() != null ? summary.name() : UNKNOWN_PLACE_NAME,
                row.category(),
                summary.latitude(),
                summary.longitude(),
                row.visitCount(),
                likedPlaceIds.contains(row.placeId()),
                firstStickerNameByPlace.get(row.placeId())
        );
    }

    private VisitedPlaceMarkerInfo toLikedOnlyMarkerInfo(Long placeId, Map<Long, PlaceSummaryInfo> summaries) {
        PlaceSummaryInfo summary = summaries.get(placeId);

        return new VisitedPlaceMarkerInfo(
                placeId,
                summary.name() != null ? summary.name() : UNKNOWN_PLACE_NAME,
                null,
                summary.latitude(),
                summary.longitude(),
                0L,
                true,
                null
        );
    }
}
