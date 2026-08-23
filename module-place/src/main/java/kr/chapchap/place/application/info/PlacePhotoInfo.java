package kr.chapchap.place.application.info;

public record PlacePhotoInfo(
        String thumbnailUrl,
        String googleMapsUri
) {

    public record PhotoMetadataInfo(
            String name,
            String googleMapsUri
    ) {
    }
}
