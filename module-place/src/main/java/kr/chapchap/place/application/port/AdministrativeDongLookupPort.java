package kr.chapchap.place.application.port;

import kr.chapchap.place.application.info.AdministrativeDongInfo;

public interface AdministrativeDongLookupPort {

    AdministrativeDongInfo findByRoadAddress(String roadAddress);

    AdministrativeDongInfo findByCoordinates(double latitude, double longitude);
}
