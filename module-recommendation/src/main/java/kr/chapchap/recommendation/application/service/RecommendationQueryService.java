package kr.chapchap.recommendation.application.service;

import kr.chapchap.recommendation.application.info.NearbyPlaceInfo;
import kr.chapchap.recommendation.application.info.PlacePopularityInfo;
import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.info.RecommendationInfo.CategoryPlacesInfo;
import kr.chapchap.recommendation.application.info.RecommendedPlaceDetailInfo;
import kr.chapchap.recommendation.application.info.RecommendedPlaceInfo;
import kr.chapchap.recommendation.application.port.CategoryCatalogLookupPort;
import kr.chapchap.recommendation.application.port.CurrentDongLookupPort;
import kr.chapchap.recommendation.application.port.PlaceLikeLookupPort;
import kr.chapchap.recommendation.application.port.PlaceRadiusLookupPort;
import kr.chapchap.recommendation.application.port.PopularityLookupPort;
import kr.chapchap.recommendation.application.port.VisitedPlaceLookupPort;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.service.PlacePhotoService;
import kr.chapchap.recommendation.exception.RecommendationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;



@RequiredArgsConstructor
@Service
public class RecommendationQueryService {


    private static final double DECAY_GRAVITY = 1.8;
    private static final int RESULT_LIMIT = 4;
    private static final double MAX_RADIUS_METERS = 50_000;
    // PlacePhotoService가 한 번에 처리 가능한 장소 수 상한(MAX_BATCH_SIZE)과 동일하게 맞춘 배치 크기
    private static final int PHOTO_BATCH_SIZE = 5;

    private final PlaceRadiusLookupPort placeRadiusLookupPort;
    private final PopularityLookupPort popularityLookupPort;
    private final PlaceLikeLookupPort placeLikeLookupPort;
    private final VisitedPlaceLookupPort visitedPlaceLookupPort;
    private final CategoryCatalogLookupPort categoryCatalogLookupPort;
    private final CurrentDongLookupPort currentDongLookupPort;
    private final PlacePhotoService placePhotoService;
    private final Clock clock;

    public RecommendationInfo getNearbyRecommendations(Long userId, double latitude, double longitude, double radiusMeters) {
        validateCoordinate(latitude, longitude);
        validateRadius(radiusMeters);

        String currentDongName = resolveCurrentDongName(latitude, longitude);
        List<String> allCategories = categoryCatalogLookupPort.findAllCategories();

        List<NearbyPlaceInfo> candidates = placeRadiusLookupPort.findWithinRadius(latitude, longitude, radiusMeters);
        if (candidates.isEmpty()) {
            return new RecommendationInfo(currentDongName, List.of(), emptyCategoryPlaces(allCategories));
        }

        List<Long> placeIds = candidates.stream().map(NearbyPlaceInfo::placeId).toList();
        List<PlacePopularityInfo> popularityRows = popularityLookupPort.aggregateByPlaceIds(placeIds);
        if (popularityRows.isEmpty()) {
            return new RecommendationInfo(currentDongName, List.of(), emptyCategoryPlaces(allCategories));
        }

        Map<Long, NearbyPlaceInfo> placesById = candidates.stream()
                .collect(Collectors.toMap(NearbyPlaceInfo::placeId, place -> place));
        Set<Long> likedPlaceIds = placeLikeLookupPort.findLikedPlaceIds(userId);
        Set<Long> visitedPlaceIds = visitedPlaceLookupPort.findVisitedPlaceIds(userId);

        List<RecommendedPlaceInfo> allSortedByPopularity = popularityRows.stream()
                .filter(row -> placesById.containsKey(row.placeId()))
                .filter(row -> !visitedPlaceIds.contains(row.placeId()))
                .sorted(Comparator.comparingDouble(this::decayScoreOf).reversed())
                .map(row -> toRecommendedPlaceInfo(row, placesById.get(row.placeId()), likedPlaceIds))
                .toList();

        Map<String, List<RecommendedPlaceInfo>> placesByCategory = allSortedByPopularity.stream()
                .filter(place -> place.category() != null)
                .collect(Collectors.groupingBy(
                        RecommendedPlaceInfo::category, LinkedHashMap::new, Collectors.toList()));

        List<CategoryPlacesInfo> categoryPlaces = allCategories.stream()
                .map(category -> new CategoryPlacesInfo(
                        category,
                        placesByCategory.getOrDefault(category, List.of()).stream().limit(RESULT_LIMIT).toList()))
                .toList();

        // 전체(allPlaces) = 카테고리별 추천을 전부 합친 것(최대 카테고리 수 × RESULT_LIMIT개), 인기순 유지
        Set<Long> categoryRecommendedPlaceIds = categoryPlaces.stream()
                .flatMap(category -> category.places().stream())
                .map(RecommendedPlaceInfo::placeId)
                .collect(Collectors.toSet());

        List<RecommendedPlaceInfo> allPlaces = allSortedByPopularity.stream()
                .filter(place -> categoryRecommendedPlaceIds.contains(place.placeId()))
                .toList();

        Map<Long, PlacePhotoInfo> thumbnails = resolveThumbnails(allPlaces);
        return new RecommendationInfo(
                currentDongName,
                applyPhotos(allPlaces, thumbnails),
                categoryPlaces.stream()
                        .map(category -> new CategoryPlacesInfo(category.category(), applyPhotos(category.places(), thumbnails)))
                        .toList()
        );
    }

    private List<CategoryPlacesInfo> emptyCategoryPlaces(List<String> allCategories) {
        return allCategories.stream().map(category -> new CategoryPlacesInfo(category, List.of())).toList();
    }

    // 동네 이름은 부가 정보라, SGIS 조회 실패해도 추천 자체는 계속 내려준다(동네 이름만 null)
    private String resolveCurrentDongName(double latitude, double longitude) {
        try {
            return currentDongLookupPort.findFullName(latitude, longitude);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public RecommendedPlaceDetailInfo getPlaceDetail(Long placeId) {
        NearbyPlaceInfo place = placeRadiusLookupPort.findById(placeId)
                .orElseThrow(() -> new BusinessException(RecommendationErrorCode.PLACE_NOT_FOUND));

        String category = popularityLookupPort.aggregateByPlaceIds(List.of(placeId)).stream()
                .findFirst()
                .map(PlacePopularityInfo::category)
                .orElse(null);

        List<PlacePhotoInfo> photos = hasGooglePlaceId(place)
                ? placePhotoService.findPhotos(Map.of(placeId, place.googlePlaceId())).getOrDefault(placeId, List.of())
                : List.of();

        List<String> thumbnailUrls = photos.stream().map(PlacePhotoInfo::thumbnailUrl).toList();
        String googleMapsUri = photos.stream()
                .map(PlacePhotoInfo::googleMapsUri)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new RecommendedPlaceDetailInfo(
                place.placeId(), place.googlePlaceId(), place.name(), category, place.dongName(), place.roadAddress(),
                place.latitude(), place.longitude(), thumbnailUrls, googleMapsUri
        );
    }

    private boolean hasGooglePlaceId(NearbyPlaceInfo place) {
        return place.googlePlaceId() != null && !place.googlePlaceId().isBlank();
    }

    private RecommendedPlaceInfo toRecommendedPlaceInfo(PlacePopularityInfo row, NearbyPlaceInfo place, Set<Long> likedPlaceIds) {
        return new RecommendedPlaceInfo(
                row.placeId(),
                place.name(),
                place.dongName(),
                row.category(),
                place.latitude(),
                place.longitude(),
                row.visitCount(),
                likedPlaceIds.contains(row.placeId()),
                null,
                null,
                place.googlePlaceId()
        );
    }

    // Google Photo 조회는 한 번에 PHOTO_BATCH_SIZE개까지만 가능해서, 중복 호출 없이 한 번만 모아서 배치로 조회한다
    private Map<Long, PlacePhotoInfo> resolveThumbnails(List<RecommendedPlaceInfo> places) {
        Map<Long, String> googlePlaceIdsByPlaceId = new LinkedHashMap<>();
        for (RecommendedPlaceInfo place : places) {
            String googlePlaceId = place.googlePlaceId();
            if (googlePlaceId != null && !googlePlaceId.isBlank()) {
                googlePlaceIdsByPlaceId.put(place.placeId(), googlePlaceId);
            }
        }
        if (googlePlaceIdsByPlaceId.isEmpty()) {
            return Map.of();
        }

        Map<Long, PlacePhotoInfo> thumbnails = new LinkedHashMap<>();
        List<Map.Entry<Long, String>> entries = List.copyOf(googlePlaceIdsByPlaceId.entrySet());
        for (int from = 0; from < entries.size(); from += PHOTO_BATCH_SIZE) {
            Map<Long, String> chunk = entries.subList(from, Math.min(from + PHOTO_BATCH_SIZE, entries.size())).stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
            thumbnails.putAll(placePhotoService.findThumbnails(chunk));
        }
        return thumbnails;
    }

    private List<RecommendedPlaceInfo> applyPhotos(List<RecommendedPlaceInfo> places, Map<Long, PlacePhotoInfo> thumbnails) {
        return places.stream()
                .map(place -> {
                    PlacePhotoInfo photo = thumbnails.get(place.placeId());
                    return photo == null
                            ? place
                            : place.withPhoto(photo.thumbnailUrl(), photo.googleMapsUri());
                })
                .toList();
    }

    private double decayScoreOf(PlacePopularityInfo row) {
        LocalDate today = LocalDate.now(clock);
        long hoursSinceLastVisit = ChronoUnit.HOURS.between(row.lastVisitedDate().atStartOfDay(), today.atStartOfDay());
        return row.visitCount() / Math.pow(hoursSinceLastVisit + 2, DECAY_GRAVITY);
    }


    private void validateCoordinate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException(RecommendationErrorCode.INVALID_COORDINATE);
        }
    }

    private void validateRadius(double radiusMeters) {
        if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RADIUS);
        }
    }
}
