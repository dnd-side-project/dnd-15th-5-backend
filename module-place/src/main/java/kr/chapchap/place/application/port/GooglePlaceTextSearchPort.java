package kr.chapchap.place.application.port;

import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;

import java.util.Optional;

public interface GooglePlaceTextSearchPort {

    Optional<GooglePlaceTextSearchInfo> searchFirst(String textQuery);
}
