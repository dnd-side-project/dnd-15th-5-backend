package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlaceLocationInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.port.PlaceLikeLookupPort;
import kr.chapchap.consumption.application.port.PlaceLocationLookupPort;
import kr.chapchap.consumption.application.port.PlaceNameLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceFirstStickerRow;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.domain.repository.StickerItemRepository;
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
    private final PlaceNameLookupPort placeNameLookupPort; //가게 이름 조회
    private final PlaceLocationLookupPort placeLocationLookupPort; //위치 정보 조회
    private final PlaceLikeLookupPort placeLikeLookupPort; //좋아요 여부 조회
    private final StickerItemRepository stickerItemRepository; //스티커 이름 조회
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
        Map<Long, String> placeNames = placeNameLookupPort.findNames(allPlaceIds);
        Map<Long, PlaceLocationInfo> placeLocations = placeLocationLookupPort.findLocations(allPlaceIds);
        Map<Long, StickerItem> firstStickerByPlace = findFirstStickerByPlace(userId, visitedPlaceIds);

        List<VisitedPlaceMarkerInfo> visitedMarkers = rows.stream()
                .map(row -> toMarkerInfo(row, placeNames, placeLocations, likedPlaceIds, firstStickerByPlace))
                .toList();

        // 방문X 좋아요
        List<VisitedPlaceMarkerInfo> likedOnlyMarkers = likedOnlyPlaceIds.stream()
                .map(placeId -> toLikedOnlyMarkerInfo(placeId, placeNames, placeLocations))
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


    private Map<Long, StickerItem> findFirstStickerByPlace(Long userId, List<Long> visitedPlaceIds) {
        if (visitedPlaceIds.isEmpty()) {
            return Map.of();
        }

        List<PlaceFirstStickerRow> firstStickerRows = consumptionQueryRepository.findFirstStickerItemIdsByPlace(userId, visitedPlaceIds);
        if (firstStickerRows.isEmpty()) {
            return Map.of();
        }

        List<Long> stickerItemIds = firstStickerRows.stream().map(PlaceFirstStickerRow::stickerItemId).distinct().toList();
        Map<Long, StickerItem> stickerItemsById = stickerItemRepository.findAllById(stickerItemIds).stream()
                .collect(Collectors.toMap(StickerItem::getId, stickerItem -> stickerItem));

        return firstStickerRows.stream()
                .collect(Collectors.toMap(PlaceFirstStickerRow::placeId,
                        row -> stickerItemsById.get(row.stickerItemId())));
    }

    private VisitedPlaceMarkerInfo toMarkerInfo(PlaceCategoryVisitRow row, Map<Long, String> placeNames,
                                                 Map<Long, PlaceLocationInfo> placeLocations, Set<Long> likedPlaceIds,
                                                 Map<Long, StickerItem> firstStickerByPlace) {
        PlaceLocationInfo location = placeLocations.get(row.placeId());
        StickerItem firstSticker = firstStickerByPlace.get(row.placeId());

        return new VisitedPlaceMarkerInfo(
                row.placeId(),
                placeNames.getOrDefault(row.placeId(), UNKNOWN_PLACE_NAME),
                row.category(),
                location.latitude(),
                location.longitude(),
                row.visitCount(),
                likedPlaceIds.contains(row.placeId()),
                firstSticker != null ? firstSticker.getName() : null
        );
    }

    private VisitedPlaceMarkerInfo toLikedOnlyMarkerInfo(Long placeId, Map<Long, String> placeNames,
                                                           Map<Long, PlaceLocationInfo> placeLocations) {
        PlaceLocationInfo location = placeLocations.get(placeId);

        return new VisitedPlaceMarkerInfo(
                placeId,
                placeNames.getOrDefault(placeId, UNKNOWN_PLACE_NAME),
                null,
                location.latitude(),
                location.longitude(),
                0L,
                true,
                null
        );
    }
}
