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
        int monthlyCount = calculateMonthlyCount(userId, currentMonth);

        //place id , 카테고리 , 횟수
        List<PlaceCategoryVisitRow> rows = consumptionQueryRepository.aggregateVisitedPlacesByCategory(userId, categories);
        if (rows.isEmpty()) {
            return new VisitedPlaceMarkersInfo(List.of(), currentMonth.getMonthValue(), monthlyCount);
        }

        List<Long> placeIds = rows.stream().map(PlaceCategoryVisitRow::placeId).toList();
        Map<Long, String> placeNames = placeNameLookupPort.findNames(placeIds);
        Map<Long, PlaceLocationInfo> placeLocations = placeLocationLookupPort.findLocations(placeIds);
        Set<Long> likedPlaceIds = placeLikeLookupPort.findLikedPlaceIds(userId, placeIds);
        Map<Long, StickerItem> firstStickerByPlace = findFirstStickerByPlace(userId, placeIds);

        List<VisitedPlaceMarkerInfo> markers = rows.stream()
                .map(row -> toMarkerInfo(row, placeNames, placeLocations, likedPlaceIds, firstStickerByPlace))
                .sorted(Comparator.comparing(VisitedPlaceMarkerInfo::visitCount).reversed())
                .toList();

        return new VisitedPlaceMarkersInfo(markers, currentMonth.getMonthValue(), monthlyCount);
    }

    private int calculateMonthlyCount(Long userId, YearMonth currentMonth) {
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEndExclusive = currentMonth.plusMonths(1).atDay(1);
        return consumptionQueryRepository.findAllByUserAndDateRange(userId, monthStart, monthEndExclusive).size();
    }


    private Map<Long, StickerItem> findFirstStickerByPlace(Long userId, List<Long> placeIds) {
        List<PlaceFirstStickerRow> firstStickerRows = consumptionQueryRepository.findFirstStickerItemIdsByPlace(userId, placeIds);
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
}
