package kr.chapchap.place.application.command;

public record PlaceResolveCommand(
        String googlePlaceId,
        String name,
        String roadAddress,
        double latitude,
        double longitude
) {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ROAD_ADDRESS_LENGTH = 255;

    public PlaceResolveCommand {
        googlePlaceId = requireText(googlePlaceId, "Google Place ID");
        name = requireText(name, "장소명");
        roadAddress = requireText(roadAddress, "도로명주소");

        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("장소명은 100자 이하여야 합니다.");
        }
        if (roadAddress.length() > MAX_ROAD_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("도로명주소는 255자 이하여야 합니다.");
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도는 -90 이상 90 이하여야 합니다.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 비어 있을 수 없습니다.");
        }
        return value.trim();
    }
}
