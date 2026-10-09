package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.info.PlacePhotoInfo.PhotoMetadataInfo;
import kr.chapchap.place.application.port.PlacePhotoPort;
import kr.chapchap.place.exception.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class PlacePhotoService {

    private static final int MAX_BATCH_SIZE = 5;
    private static final int THUMBNAIL_MAX_WIDTH_PX = 400;
    private static final int THUMBNAIL_PHOTO_COUNT = 1;
    private static final int GALLERY_PHOTO_COUNT = 3;

    private final PlacePhotoPort placePhotoPort;
    private final Executor executor;

    public Map<Long, PlacePhotoInfo> findThumbnails(Map<Long, String> googlePlaceIdsByPlaceId) {
        Map<Long, PlacePhotoInfo> thumbnails = new LinkedHashMap<>();
        findPhotos(googlePlaceIdsByPlaceId, THUMBNAIL_PHOTO_COUNT)
                .forEach((placeId, photos) -> thumbnails.put(placeId, photos.getFirst()));
        return thumbnails;
    }

    public Map<Long, List<PlacePhotoInfo>> findPhotos(Map<Long, String> googlePlaceIdsByPlaceId) {
        return findPhotos(googlePlaceIdsByPlaceId, GALLERY_PHOTO_COUNT);
    }

    private Map<Long, List<PlacePhotoInfo>> findPhotos(
            Map<Long, String> googlePlaceIdsByPlaceId,
            int requestedPhotoCount
    ) {
        Objects.requireNonNull(googlePlaceIdsByPlaceId);
        if (googlePlaceIdsByPlaceId.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("사진은 한 번에 최대 5개 장소까지 조회할 수 있습니다.");
        }
        if (googlePlaceIdsByPlaceId.isEmpty()) {
            return Map.of();
        }

        Map<Long, CompletableFuture<List<PlacePhotoInfo>>> futures = new LinkedHashMap<>();
        googlePlaceIdsByPlaceId.forEach((placeId, googlePlaceId) -> futures.put(
                placeId,
                CompletableFuture.supplyAsync(
                        () -> findPhotos(placeId, googlePlaceId, requestedPhotoCount),
                        executor
                )
        ));

        Map<Long, List<PlacePhotoInfo>> photosByPlaceId = new LinkedHashMap<>();
        futures.forEach((placeId, future) -> {
            List<PlacePhotoInfo> photos = future.join();
            if (!photos.isEmpty()) {
                photosByPlaceId.put(placeId, photos);
            }
        });
        return photosByPlaceId;
    }

    private List<PlacePhotoInfo> findPhotos(Long placeId, String googlePlaceId, int requestedPhotoCount) {
        if (placeId == null || googlePlaceId == null || googlePlaceId.isBlank()) {
            return List.of();
        }

        List<PhotoMetadataInfo> metadata;
        try {
            metadata = requestedPhotoCount == THUMBNAIL_PHOTO_COUNT
                    ? placePhotoPort.findPrimaryPhoto(googlePlaceId.trim()).stream().toList()
                    : placePhotoPort.findPhotos(googlePlaceId.trim(), requestedPhotoCount);
        } catch (BusinessException exception) {
            logPhotoFailure(placeId, exception);
            return List.of();
        }

        List<PlacePhotoInfo> photos = new ArrayList<>();
        for (PhotoMetadataInfo photo : metadata) {
            try {
                photos.add(new PlacePhotoInfo(
                        placePhotoPort.resolvePhotoUri(photo.name(), THUMBNAIL_MAX_WIDTH_PX).toString(),
                        photo.googleMapsUri()
                ));
            } catch (BusinessException exception) {
                logPhotoFailure(placeId, exception);
                if (exception.getErrorCode() == PlaceErrorCode.PHOTO_REQUEST_LIMIT_EXCEEDED) {
                    break;
                }
            }
        }
        return List.copyOf(photos);
    }

    private void logPhotoFailure(Long placeId, BusinessException exception) {
        log.warn(
                "장소 사진 조회에 실패했습니다. placeId={}, code={}",
                placeId,
                exception.getErrorCode().getCode()
        );
    }
}
