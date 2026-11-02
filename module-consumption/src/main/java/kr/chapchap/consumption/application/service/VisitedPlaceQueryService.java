package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlacePopularityInfo;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.port.PlaceLikeLookupPort;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceLatestImageRow;
import kr.chapchap.consumption.domain.entity.PlaceLatestStickerRow;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
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
    private final StickerQueryService stickerQueryService; //스티커 조회
    private final ConsumptionImageRepository consumptionImageRepository; //사진 조회
    private final ConsumptionImageStorage consumptionImageStorage; //사진 URL 변환
    private final PopularityQueryService popularityQueryService; //좋아요만 한 가게의 카테고리 조회(전체 유저 소비기록 기준)
    private final Clock clock;

    public VisitedPlaceMarkersInfo getVisitedPlaceMarkers(Long userId, List<String> categories) {
        YearMonth currentMonth = YearMonth.now(clock);
        int monthlyPlaceCount = calculateMonthlyPlaceCount(userId, currentMonth);

        //방문 한 곳
        List<PlaceCategoryVisitRow> rows = consumptionQueryRepository.aggregateVisitedPlacesByCategory(userId, categories);
        List<Long> visitedPlaceIds = rows.stream().map(PlaceCategoryVisitRow::placeId).toList();
        Set<Long> visitedPlaceIdSet = Set.copyOf(visitedPlaceIds);

        Set<Long> likedPlaceIds = placeLikeLookupPort.findLikedPlaceIds(userId); //좋아요
        List<Long> likedOnlyCandidateIds = likedPlaceIds.stream().filter(placeId -> !visitedPlaceIdSet.contains(placeId)).toList(); //방문x 좋아요

        // 방문X 좋아요도 카테고리 필터를 똑같이 적용한다 (필터링한 카테고리와 다른 좋아요 장소는 제외)
        Map<Long, String> categoriesByLikedOnlyPlace = resolveLikedOnlyCategories(likedOnlyCandidateIds);
        List<Long> likedOnlyPlaceIds = filterByCategories(likedOnlyCandidateIds, categoriesByLikedOnlyPlace, categories);

        if (rows.isEmpty() && likedOnlyPlaceIds.isEmpty()) {
            return new VisitedPlaceMarkersInfo(List.of(), currentMonth.getMonthValue(), monthlyPlaceCount);
        }

        List<Long> allPlaceIds = Stream.concat(visitedPlaceIds.stream(), likedOnlyPlaceIds.stream()).toList();
        Map<Long, PlaceSummaryInfo> summaries = placeSummaryLookupPort.findSummaries(allPlaceIds);
        requireAllLocationsPresent(allPlaceIds, summaries);
        Map<Long, PlacePinVisual> pinVisualsByPlace = resolvePinVisuals(userId, visitedPlaceIds);

        List<VisitedPlaceMarkerInfo> visitedMarkers = rows.stream()
                .map(row -> toMarkerInfo(row, summaries, likedPlaceIds, pinVisualsByPlace))
                .toList();

        List<VisitedPlaceMarkerInfo> likedOnlyMarkers = likedOnlyPlaceIds.stream()
                .map(placeId -> toLikedOnlyMarkerInfo(placeId, summaries, categoriesByLikedOnlyPlace))
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


    // 핀 비주얼: 가게별 사진 기록이 있으면 최신 사진, 없으면 최신 스티커
    private Map<Long, PlacePinVisual> resolvePinVisuals(Long userId, List<Long> visitedPlaceIds) {
        if (visitedPlaceIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> imageUrlsByPlace = resolveLatestImageUrls(userId, visitedPlaceIds);
        List<Long> placeIdsWithoutImage = visitedPlaceIds.stream()
                .filter(placeId -> !imageUrlsByPlace.containsKey(placeId))
                .toList();
        Map<Long, StickerItem> stickersByPlace = resolveLatestStickers(userId, placeIdsWithoutImage);

        Map<Long, PlacePinVisual> visuals = new java.util.HashMap<>();
        for (Long placeId : visitedPlaceIds) {
            String imageUrl = imageUrlsByPlace.get(placeId);
            if (imageUrl != null) {
                visuals.put(placeId, new PlacePinVisual(null, imageUrl));
                continue;
            }
            StickerItem stickerItem = stickersByPlace.get(placeId);
            if (stickerItem != null) {
                visuals.put(placeId, new PlacePinVisual(stickerItem, null));
            }
        }
        return visuals;
    }

    private Map<Long, String> resolveLatestImageUrls(Long userId, List<Long> placeIds) {
        List<PlaceLatestImageRow> rows = consumptionQueryRepository.findLatestImageIdsByPlace(userId, placeIds);
        if (rows.isEmpty()) {
            return Map.of();
        }

        List<Long> imageIds = rows.stream().map(PlaceLatestImageRow::imageId).toList();
        Map<Long, ConsumptionImage> imagesById = consumptionImageRepository.findAllByIdInAndUserId(imageIds, userId)
                .stream()
                .collect(Collectors.toMap(ConsumptionImage::getId, image -> image));

        Map<Long, String> urlsByPlace = new java.util.HashMap<>();
        for (PlaceLatestImageRow row : rows) {
            ConsumptionImage image = imagesById.get(row.imageId());
            if (image != null) {
                urlsByPlace.put(row.placeId(), consumptionImageStorage.createReadUrl(image.getObjectKey()));
            }
        }
        return urlsByPlace;
    }

    private Map<Long, StickerItem> resolveLatestStickers(Long userId, List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }

        List<PlaceLatestStickerRow> rows = consumptionQueryRepository.findLatestStickerItemsByPlace(userId, placeIds);
        if (rows.isEmpty()) {
            return Map.of();
        }

        List<Long> stickerItemIds = rows.stream().map(PlaceLatestStickerRow::stickerItemId).toList();
        Map<Long, StickerItem> stickerItemsById = stickerQueryService.findItems(stickerItemIds);

        return rows.stream()
                .filter(row -> stickerItemsById.containsKey(row.stickerItemId()))
                .collect(Collectors.toMap(PlaceLatestStickerRow::placeId,
                        row -> stickerItemsById.get(row.stickerItemId())));
    }

    private record PlacePinVisual(StickerItem stickerItem, String imageUrl) {
    }

    // 좋아요만 하고 방문한 적 없는 가게도, 다른 유저의 소비기록이 있으면 그걸 기준으로 카테고리를 채운다
    private Map<Long, String> resolveLikedOnlyCategories(List<Long> likedOnlyPlaceIds) {
        if (likedOnlyPlaceIds.isEmpty()) {
            return Map.of();
        }

        return popularityQueryService.aggregatePopularityByPlaceIds(likedOnlyPlaceIds).stream()
                .collect(Collectors.toMap(PlacePopularityInfo::placeId, PlacePopularityInfo::category));
    }


    private List<Long> filterByCategories(List<Long> placeIds, Map<Long, String> categoriesByPlace, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return placeIds;
        }
        Set<String> categorySet = Set.copyOf(categories);
        return placeIds.stream()
                .filter(placeId -> categorySet.contains(categoriesByPlace.get(placeId)))
                .toList();
    }

    private void requireAllLocationsPresent(List<Long> placeIds, Map<Long, PlaceSummaryInfo> summaries) {
        boolean anyMissing = placeIds.stream().distinct()
                .anyMatch(placeId -> {
                    PlaceSummaryInfo summary = summaries.get(placeId);
                    return summary == null || summary.latitude() == null || summary.longitude() == null;
                });
        if (anyMissing) {
            throw new BusinessException(ConsumptionErrorCode.PLACE_LOCATION_NOT_FOUND);
        }
    }

    private VisitedPlaceMarkerInfo toMarkerInfo(PlaceCategoryVisitRow row, Map<Long, PlaceSummaryInfo> summaries,
                                                 Set<Long> likedPlaceIds, Map<Long, PlacePinVisual> pinVisualsByPlace) {
        PlaceSummaryInfo summary = summaries.get(row.placeId());
        PlacePinVisual visual = pinVisualsByPlace.get(row.placeId());
        StickerItem stickerItem = visual != null ? visual.stickerItem() : null;

        return new VisitedPlaceMarkerInfo(
                row.placeId(),
                summary.name() != null ? summary.name() : UNKNOWN_PLACE_NAME,
                row.category(),
                summary.latitude(),
                summary.longitude(),
                row.visitCount(),
                likedPlaceIds.contains(row.placeId()),
                stickerItem != null ? stickerItem.getName() : null,
                visual != null ? visual.imageUrl() : null,
                summary.googlePlaceId()
        );
    }

    private VisitedPlaceMarkerInfo toLikedOnlyMarkerInfo(Long placeId, Map<Long, PlaceSummaryInfo> summaries,
                                                           Map<Long, String> categoriesByLikedOnlyPlace) {
        PlaceSummaryInfo summary = summaries.get(placeId);

        return new VisitedPlaceMarkerInfo(
                placeId,
                summary.name() != null ? summary.name() : UNKNOWN_PLACE_NAME,
                categoriesByLikedOnlyPlace.get(placeId),
                summary.latitude(),
                summary.longitude(),
                0L,
                true,
                null,
                null,
                summary.googlePlaceId()
        );
    }
}
