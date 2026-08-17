package kr.chapchap.consumption.application.command;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;

public record PlaceResolveCommand(
        String googlePlaceId,
        String placeName,
        String roadAddress,
        Double latitude,
        Double longitude
) {

    private static final int MAX_PLACE_NAME_LENGTH = 100;
    private static final int MAX_ROAD_ADDRESS_LENGTH = 255;

    public PlaceResolveCommand {
        googlePlaceId = trim(googlePlaceId);
        placeName = trim(placeName);
        roadAddress = trim(roadAddress);

        if (googlePlaceId == null || googlePlaceId.isBlank()
                || placeName == null || placeName.isBlank() || placeName.length() > MAX_PLACE_NAME_LENGTH
                || roadAddress == null || roadAddress.isBlank() || roadAddress.length() > MAX_ROAD_ADDRESS_LENGTH
                || latitude == null || !Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || longitude == null || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_CONSUMPTION_INPUT);
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
