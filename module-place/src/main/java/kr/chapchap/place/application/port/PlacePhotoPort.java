package kr.chapchap.place.application.port;

import kr.chapchap.place.application.info.PlacePhotoInfo.PhotoMetadataInfo;

import java.net.URI;
import java.util.Optional;

public interface PlacePhotoPort {

    Optional<PhotoMetadataInfo> findPrimaryPhoto(String googlePlaceId);

    URI resolvePhotoUri(String photoName, int maxWidthPx);
}
