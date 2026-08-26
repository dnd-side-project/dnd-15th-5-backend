package kr.chapchap.consumption.application.info;

public record VisitedPlaceMarkerInfo(
        Long placeId,
        String placeName,
        String category,
        Double latitude,
        Double longitude,
        long visitCount,
        boolean liked,
        String stickerCategory,
        String stickerName
) {
}
