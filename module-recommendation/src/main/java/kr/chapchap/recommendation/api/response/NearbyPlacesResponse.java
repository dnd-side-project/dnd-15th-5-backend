package kr.chapchap.recommendation.api.response;

import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.info.RecommendedPlaceInfo;

import java.util.List;

public record NearbyPlacesResponse(
        List<RecommendedPlaceItem> myTownPlaces,
        List<RecommendedPlaceItem> sameCategoryPlaces
) {

    public static NearbyPlacesResponse from(RecommendationInfo info) {
        return new NearbyPlacesResponse(
                info.myTownPlaces().stream().map(RecommendedPlaceItem::from).toList(),
                info.sameCategoryPlaces().stream().map(RecommendedPlaceItem::from).toList()
        );
    }

    public record RecommendedPlaceItem(
            Long placeId,
            String name,
            String dongName,
            String category,
            Double latitude,
            Double longitude,
            Long visitCount,
            boolean liked,
            String thumbnailUrl,
            String googleMapsUri,
            String googlePlaceId
    ) {
        public static RecommendedPlaceItem from(RecommendedPlaceInfo info) {
            return new RecommendedPlaceItem(
                    info.placeId(), info.name(), info.dongName(), info.category(),
                    info.latitude(), info.longitude(), info.visitCount(), info.liked(),
                    info.thumbnailUrl(), info.googleMapsUri(), info.googlePlaceId());
        }
    }
}
