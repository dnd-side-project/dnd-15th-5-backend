package kr.chapchap.consumption.application.service;

import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.service.PlacePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
class PlaceThumbnailBatchLookup {

    private static final int PHOTO_BATCH_SIZE = 5;

    private final PlacePhotoService placePhotoService;

    Map<Long, PlacePhotoInfo> findThumbnails(Map<Long, String> googlePlaceIdsByPlaceId) {
        if (googlePlaceIdsByPlaceId.isEmpty()) {
            return Map.of();
        }

        Map<Long, PlacePhotoInfo> thumbnails = new LinkedHashMap<>();
        List<Long> placeIds = new ArrayList<>(googlePlaceIdsByPlaceId.keySet());
        for (int from = 0; from < placeIds.size(); from += PHOTO_BATCH_SIZE) {
            int to = Math.min(from + PHOTO_BATCH_SIZE, placeIds.size());
            Map<Long, String> chunk = new LinkedHashMap<>();
            for (Long placeId : placeIds.subList(from, to)) {
                chunk.put(placeId, googlePlaceIdsByPlaceId.get(placeId));
            }
            thumbnails.putAll(placePhotoService.findThumbnails(chunk));
        }
        return thumbnails;
    }
}
