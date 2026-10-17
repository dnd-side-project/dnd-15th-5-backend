package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.command.LikedPlaceSearchCommand;
import kr.chapchap.place.application.info.LikedPlaceScrollInfo;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.info.PlaceSummaryInfo;
import kr.chapchap.place.application.port.PlaceCategoryLookupPort;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceLikeQueryServiceTest {

    @Mock private PlaceLikeRepository likes;
    @Mock private PlaceCategoryLookupPort categories;
    @Mock private PlaceQueryService places;
    @Mock private PlacePhotoService photos;

    private PlaceLikeQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceLikeQueryService(likes, categories, places, photos);
    }

    @Test
    void 필터를_먼저_적용하고_페이지_장소만_사진을_조회하되_필터목록은_전체에서_구한다() {
        // given
        when(likes.findByUserIdOrderByIdDesc(1L))
                .thenReturn(List.of(like(5L, 101L), like(4L, 102L), like(3L, 103L), like(2L, 104L)));
        when(categories.findCategoriesByPlaceIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(Map.of(101L, "쇼핑", 102L, "카페", 103L, "카페"));
        when(places.findSummariesByIds(List.of(102L))).thenReturn(Map.of(102L, summary("google-102")));
        PlacePhotoInfo photo = new PlacePhotoInfo("photo-url", "maps-url");
        when(photos.findPhotos(Map.of(102L, "google-102"))).thenReturn(Map.of(102L, List.of(photo)));

        // when
        LikedPlaceScrollInfo result = sut.findLikedPlaces(new LikedPlaceSearchCommand(1L, "카페", null, 1));

        // then
        assertThat(result.categories()).containsExactly("쇼핑", "카페");
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursorId()).isEqualTo(4L);
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.placeId()).isEqualTo(102L);
            assertThat(place.category()).isEqualTo("카페");
            assertThat(place.photos()).containsExactly(photo);
        });
        verify(photos).findPhotos(Map.of(102L, "google-102"));
    }

    @Test
    void 커서의_좋아요를_해제해도_다음_페이지를_조회하며_기록없는_장소와_사진없는_장소도_남는다() {
        // given
        when(likes.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(like(5L, 101L), like(2L, 104L)));
        when(categories.findCategoriesByPlaceIds(List.of(101L, 104L))).thenReturn(Map.of(101L, "카페"));
        when(places.findSummariesByIds(List.of(104L))).thenReturn(Map.of(104L, summary("google-104")));
        when(photos.findPhotos(Map.of(104L, "google-104"))).thenReturn(Map.of());

        // when
        LikedPlaceScrollInfo result = sut.findLikedPlaces(new LikedPlaceSearchCommand(1L, null, 4L, 5));

        // then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursorId()).isNull();
        assertThat(result.categories()).containsExactly("카페");
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.placeId()).isEqualTo(104L);
            assertThat(place.category()).isNull();
            assertThat(place.photos()).isEmpty();
        });
    }

    @Test
    void 선택한_카테고리에_장소가_없어도_전체_필터목록은_반환한다() {
        // given
        when(likes.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(like(5L, 101L)));
        when(categories.findCategoriesByPlaceIds(List.of(101L))).thenReturn(Map.of(101L, "카페"));

        // when
        LikedPlaceScrollInfo result = sut.findLikedPlaces(new LikedPlaceSearchCommand(1L, "쇼핑", null, 5));

        // then
        assertThat(result.places()).isEmpty();
        assertThat(result.categories()).containsExactly("카페");
        verifyNoInteractions(places, photos);
    }

    @Test
    void 좋아요가_없으면_집계와_사진을_조회하지_않는다() {
        // given
        when(likes.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of());

        // when
        LikedPlaceScrollInfo result = sut.findLikedPlaces(new LikedPlaceSearchCommand(1L, null, null, 5));

        // then
        assertThat(result.places()).isEmpty();
        assertThat(result.categories()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        verifyNoInteractions(categories, places, photos);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 100})
    void 페이지_크기가_범위를_벗어나면_거부한다(int size) {
        // given & when & then
        assertThatThrownBy(() -> new LikedPlaceSearchCommand(1L, null, null, size))
                .isInstanceOf(BusinessException.class);
    }

    private PlaceLike like(Long id, Long placeId) {
        PlaceLike like = PlaceLike.builder().userId(1L).placeId(placeId).build();
        ReflectionTestUtils.setField(like, "id", id);
        return like;
    }

    private PlaceSummaryInfo summary(String googlePlaceId) {
        return new PlaceSummaryInfo("가게", "역삼동", "서울특별시 강남구", 37.5, 127.0, googlePlaceId);
    }
}
