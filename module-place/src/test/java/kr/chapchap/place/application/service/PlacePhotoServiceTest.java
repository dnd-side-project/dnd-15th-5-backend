package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.info.PlacePhotoInfo.PhotoMetadataInfo;
import kr.chapchap.place.application.port.PlacePhotoPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacePhotoServiceTest {

    private static final int THUMBNAIL_WIDTH = 400;

    @Mock
    private PlacePhotoPort placePhotoPort;

    private ExecutorService executor;
    private PlacePhotoService sut;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        sut = new PlacePhotoService(placePhotoPort, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void 여러_장소의_사진을_조회할_때_사진이_있는_장소만_thumbnailUrl과_googleMapsUri를_반환한다() {
        // given
        PhotoMetadataInfo firstPhoto = new PhotoMetadataInfo(
                "places/google-101/photos/photo-101",
                "https://maps.google.com/photo"
        );
        URI photoUri = URI.create("https://lh3.googleusercontent.com/photo-101");
        when(placePhotoPort.findPrimaryPhoto("google-101")).thenReturn(Optional.of(firstPhoto));
        when(placePhotoPort.resolvePhotoUri(firstPhoto.name(), THUMBNAIL_WIDTH)).thenReturn(photoUri);
        when(placePhotoPort.findPrimaryPhoto("google-102")).thenReturn(Optional.empty());

        // when
        Map<Long, PlacePhotoInfo> result = sut.findThumbnails(new LinkedHashMap<>(Map.of(
                101L, "google-101",
                102L, "google-102"
        )));

        // then
        assertThat(result).containsOnlyKeys(101L);
        PlacePhotoInfo photoInfo = result.get(101L);
        assertThat(photoInfo.thumbnailUrl()).isEqualTo(photoUri.toString());
        assertThat(photoInfo.googleMapsUri()).isEqualTo(firstPhoto.googleMapsUri());
        verify(placePhotoPort).resolvePhotoUri(firstPhoto.name(), THUMBNAIL_WIDTH);
    }

    @Test
    void 여러_장소의_사진을_조회할_때_한_장소의_조회가_실패해도_다른_장소의_사진은_반환한다() {
        // given
        when(placePhotoPort.findPrimaryPhoto("google-101"))
                .thenThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));
        when(placePhotoPort.findPrimaryPhoto("google-102")).thenReturn(Optional.of(new PhotoMetadataInfo(
                "places/google-102/photos/photo-102",
                "https://maps.google.com/photo-102"
        )));
        when(placePhotoPort.resolvePhotoUri(
                "places/google-102/photos/photo-102",
                THUMBNAIL_WIDTH
        )).thenReturn(URI.create("https://lh3.googleusercontent.com/photo-102"));

        // when
        Map<Long, PlacePhotoInfo> result = sut.findThumbnails(Map.of(
                101L, "google-101",
                102L, "google-102",
                103L, " "
        ));

        // then
        assertThat(result).containsOnlyKeys(102L);
        verify(placePhotoPort, never()).findPrimaryPhoto(" ");
    }

    @Test
    void 장소_사진을_조회할_때_장소가_5개면_모든_작업을_제출한_뒤_결과를_기다린다() {
        // given
        List<Runnable> submittedTasks = new ArrayList<>();
        Executor batchingExecutor = task -> {
            submittedTasks.add(task);
            if (submittedTasks.size() == 5) {
                submittedTasks.forEach(Runnable::run);
            }
        };
        when(placePhotoPort.findPrimaryPhoto(anyString())).thenAnswer(invocation -> {
            String googlePlaceId = invocation.getArgument(0);
            return Optional.of(new PhotoMetadataInfo(
                    "places/" + googlePlaceId + "/photos/primary",
                    "https://maps.google.com/photo/" + googlePlaceId
            ));
        });
        when(placePhotoPort.resolvePhotoUri(anyString(), eq(THUMBNAIL_WIDTH)))
                .thenReturn(URI.create("https://lh3.googleusercontent.com/photo"));
        PlacePhotoService concurrentService = new PlacePhotoService(placePhotoPort, batchingExecutor);

        // when
        Map<Long, PlacePhotoInfo> result = concurrentService.findThumbnails(Map.of(
                1L, "google-1",
                2L, "google-2",
                3L, "google-3",
                4L, "google-4",
                5L, "google-5"
        ));

        // then
        assertThat(submittedTasks).hasSize(5);
        assertThat(result).hasSize(5);
    }

    @Test
    void 장소_사진을_조회할_때_장소가_6개면_요청을_거부한다() {
        // given
        Map<Long, String> places = Map.of(
                1L, "google-1",
                2L, "google-2",
                3L, "google-3",
                4L, "google-4",
                5L, "google-5",
                6L, "google-6"
        );

        // when & then
        assertThatThrownBy(() -> sut.findThumbnails(places))
                .isInstanceOf(IllegalArgumentException.class);
        verify(placePhotoPort, never()).findPrimaryPhoto(anyString());
    }
}
