package kr.chapchap.recommendation.application.service;

import kr.chapchap.recommendation.application.info.NearbyPlaceInfo;
import kr.chapchap.recommendation.application.info.PlacePopularityInfo;
import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.info.RecommendedPlaceInfo;
import kr.chapchap.recommendation.application.port.PlaceLikeLookupPort;
import kr.chapchap.recommendation.application.port.PlaceRadiusLookupPort;
import kr.chapchap.recommendation.application.port.PopularityLookupPort;
import kr.chapchap.recommendation.application.port.UserTopCategoryLookupPort;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;



@RequiredArgsConstructor
@Service
public class RecommendationQueryService {


    private static final double DECAY_GRAVITY = 1.8;
    private static final int RESULT_LIMIT = 2;
    private static final double MAX_RADIUS_METERS = 50_000;

    private final PlaceRadiusLookupPort placeRadiusLookupPort;
    private final PopularityLookupPort popularityLookupPort;
    private final UserTopCategoryLookupPort userTopCategoryLookupPort;
    private final PlaceLikeLookupPort placeLikeLookupPort;
    private final VisitedPlaceLookupPort visitedPlaceLookupPort;
    private final PlacePhotoService placePhotoService;
    private final Clock clock;

    public RecommendationInfo getNearbyRecommendations(Long userId, double latitude, double longitude, double radiusMeters) {
        validateCoordinate(latitude, longitude);
        validateRadius(radiusMeters);

        List<NearbyPlaceInfo> candidates = placeRadiusLookupPort.findWithinRadius(latitude, longitude, radiusMeters);
        if (candidates.isEmpty()) {
            return new RecommendationInfo(List.of(), List.of());
        }

        List<Long> placeIds = candidates.stream().map(NearbyPlaceInfo::placeId).toList();
        List<PlacePopularityInfo> popularityRows = popularityLookupPort.aggregateByPlaceIds(placeIds);
        if (popularityRows.isEmpty()) {
            return new RecommendationInfo(List.of(), List.of());
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

        List<RecommendedPlaceInfo> myTownPlaces = allSortedByPopularity.stream()
                .limit(RESULT_LIMIT)
                .toList();

        Set<Long> myTownPlaceIds = myTownPlaces.stream().map(RecommendedPlaceInfo::placeId).collect(Collectors.toSet());
        Optional<String> topCategory = userTopCategoryLookupPort.findTopCategory(userId);
        List<RecommendedPlaceInfo> sameCategoryPlaces = topCategory
                .map(category -> allSortedByPopularity.stream()
                        .filter(place -> category.equals(place.category()))
                        .filter(place -> !myTownPlaceIds.contains(place.placeId()))
                        .limit(RESULT_LIMIT)
                        .toList())
                .orElse(List.of());

        return new RecommendationInfo(
                withPhotos(myTownPlaces, placesById),
                withPhotos(sameCategoryPlaces, placesById)
        );
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
                null
        );
    }

    private List<RecommendedPlaceInfo> withPhotos(
            List<RecommendedPlaceInfo> places,
            Map<Long, NearbyPlaceInfo> placesById
    ) {
        if (places.isEmpty()) {
            return places;
        }

        Map<Long, String> googlePlaceIdsByPlaceId = new LinkedHashMap<>();
        for (RecommendedPlaceInfo place : places) {
            String googlePlaceId = placesById.get(place.placeId()).googlePlaceId();
            if (googlePlaceId != null && !googlePlaceId.isBlank()) {
                googlePlaceIdsByPlaceId.put(place.placeId(), googlePlaceId);
            }
        }
        if (googlePlaceIdsByPlaceId.isEmpty()) {
            return places;
        }

        Map<Long, PlacePhotoInfo> thumbnails = placePhotoService.findThumbnails(googlePlaceIdsByPlaceId);
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
