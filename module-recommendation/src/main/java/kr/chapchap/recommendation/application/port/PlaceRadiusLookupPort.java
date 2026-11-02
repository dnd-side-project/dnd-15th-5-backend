package kr.chapchap.recommendation.application.port;

import kr.chapchap.recommendation.application.info.NearbyPlaceInfo;

import java.util.List;
import java.util.Optional;

public interface PlaceRadiusLookupPort {

    List<NearbyPlaceInfo> findWithinRadius(double latitude, double longitude, double radiusMeters);

    Optional<NearbyPlaceInfo> findById(Long placeId);
}
