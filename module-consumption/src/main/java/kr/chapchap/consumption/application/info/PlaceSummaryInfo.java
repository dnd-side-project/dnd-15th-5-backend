package kr.chapchap.consumption.application.info;

public record PlaceSummaryInfo(
        String name,
        String dongName,
        String address,
        Double latitude,
        Double longitude,
        String googlePlaceId
) {

    public PlaceSummaryInfo(
            String name,
            String dongName,
            String address,
            Double latitude,
            Double longitude
    ) {
        this(name, dongName, address, latitude, longitude, null);
    }
}
