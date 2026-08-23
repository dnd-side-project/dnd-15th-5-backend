package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.info.PlacePhotoInfo.PhotoMetadataInfo;
import kr.chapchap.place.application.port.PlacePhotoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class PlacePhotoService {

    private static final int MAX_BATCH_SIZE = 5;
    private static final int THUMBNAIL_MAX_WIDTH_PX = 400;

    private final PlacePhotoPort placePhotoPort;
    private final Executor executor;

    public Map<Long, PlacePhotoInfo> findThumbnails(Map<Long, String> googlePlaceIdsByPlaceId) {
        Objects.requireNonNull(googlePlaceIdsByPlaceId);
        if (googlePlaceIdsByPlaceId.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("사진은 한 번에 최대 5개 장소까지 조회할 수 있습니다.");
        }
        if (googlePlaceIdsByPlaceId.isEmpty()) {
            return Map.of();
        }

        List<CompletableFuture<Optional<ThumbnailEntry>>> futures = new ArrayList<>();
        googlePlaceIdsByPlaceId.forEach((placeId, googlePlaceId) -> futures.add(
                CompletableFuture.supplyAsync(
                        () -> findThumbnail(placeId, googlePlaceId),
                        executor
                )
        ));

        Map<Long, PlacePhotoInfo> thumbnails = new LinkedHashMap<>();
        futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .forEach(entry -> thumbnails.put(entry.placeId(), entry.photoInfo()));
        return thumbnails;
    }

    private Optional<ThumbnailEntry> findThumbnail(Long placeId, String googlePlaceId) {
        if (placeId == null || googlePlaceId == null || googlePlaceId.isBlank()) {
            return Optional.empty();
        }

        try {
            return placePhotoPort.findPrimaryPhoto(googlePlaceId.trim())
                    .map(photo -> new ThumbnailEntry(
                            placeId,
                            new PlacePhotoInfo(
                                    placePhotoPort.resolvePhotoUri(
                                            photo.name(),
                                            THUMBNAIL_MAX_WIDTH_PX
                                    ).toString(),
                                    photo.googleMapsUri()
                            )
                    ));
        } catch (BusinessException exception) {
            log.warn(
                    "장소 썸네일 조회에 실패했습니다. placeId={}, code={}",
                    placeId,
                    exception.getErrorCode().getCode()
            );
            return Optional.empty();
        }
    }

    private record ThumbnailEntry(Long placeId, PlacePhotoInfo photoInfo) {
    }
}
