package kr.chapchap.consumption.application.info;

public record FrequentPlaceInfo(
        int rank,
        Long placeId,
        String placeName,
        String category,
        String dongName,
        long visitCount
) {
}
