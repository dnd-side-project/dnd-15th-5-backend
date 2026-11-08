package kr.chapchap.recommendation.application.service;

import kr.chapchap.place.application.service.PlacePhotoService;
import kr.chapchap.recommendation.application.info.NearbyPlaceInfo;
import kr.chapchap.recommendation.application.info.PlacePopularityInfo;
import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.port.CategoryCatalogLookupPort;
import kr.chapchap.recommendation.application.port.CurrentDongLookupPort;
import kr.chapchap.recommendation.application.port.PlaceLikeLookupPort;
import kr.chapchap.recommendation.application.port.PlaceRadiusLookupPort;
import kr.chapchap.recommendation.application.port.PopularityLookupPort;
import kr.chapchap.recommendation.application.port.VisitedPlaceLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationQueryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-08-13T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private PlaceRadiusLookupPort placeRadiusLookupPort;

    @Mock
    private PopularityLookupPort popularityLookupPort;

    @Mock
    private PlaceLikeLookupPort placeLikeLookupPort;

    @Mock
    private VisitedPlaceLookupPort visitedPlaceLookupPort;

    @Mock
    private CategoryCatalogLookupPort categoryCatalogLookupPort;

    @Mock
    private CurrentDongLookupPort currentDongLookupPort;

    @Mock
    private PlacePhotoService placePhotoService;

    private RecommendationQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new RecommendationQueryService(
                placeRadiusLookupPort, popularityLookupPort,
                placeLikeLookupPort, visitedPlaceLookupPort, categoryCatalogLookupPort, currentDongLookupPort,
                placePhotoService, fixedClock);
        when(categoryCatalogLookupPort.findAllCategories()).thenReturn(List.of("카페", "음식점", "운동"));
    }

    @Test
    void 반경_안_장소의_googlePlaceId를_결과에_그대로_담는다() {
        // given
        when(placeRadiusLookupPort.findWithinRadius(37.5665, 126.9780, 1000)).thenReturn(List.of(
                new NearbyPlaceInfo(101L, "투썸플레이스", "역삼동", null, 37.5447, 127.0557, "ChIJtest101"),
                new NearbyPlaceInfo(102L, "국밥집", "역삼동", null, 37.4999, 127.0364, null)
        ));
        when(popularityLookupPort.aggregateByPlaceIds(any())).thenReturn(List.of(
                new PlacePopularityInfo(101L, "카페", 5L, LocalDate.of(2026, 8, 1)),
                new PlacePopularityInfo(102L, "음식점", 3L, LocalDate.of(2026, 8, 1))
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(visitedPlaceLookupPort.findVisitedPlaceIds(1L)).thenReturn(Set.of());
        when(placePhotoService.findThumbnails(any())).thenReturn(Map.of());

        // when
        RecommendationInfo info = sut.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then — googlePlaceId가 있는 장소는 그대로 내려오고, 없는 장소는 null이어야 함
        String googlePlaceId101 = info.allPlaces().stream()
                .filter(place -> place.placeId().equals(101L))
                .findFirst().orElseThrow()
                .googlePlaceId();
        assertThat(googlePlaceId101).isEqualTo("ChIJtest101");

        String googlePlaceId102 = info.allPlaces().stream()
                .filter(place -> place.placeId().equals(102L))
                .findFirst().orElseThrow()
                .googlePlaceId();
        assertThat(googlePlaceId102).isNull();
    }

    @Test
    void 전체는_카테고리별_추천을_합친_것이다() {
        // given
        when(placeRadiusLookupPort.findWithinRadius(37.5665, 126.9780, 1000)).thenReturn(List.of(
                new NearbyPlaceInfo(101L, "투썸플레이스", "역삼동", null, 37.5447, 127.0557, "ChIJtest101"),
                new NearbyPlaceInfo(102L, "국밥집", "역삼동", null, 37.4999, 127.0364, null)
        ));
        when(popularityLookupPort.aggregateByPlaceIds(any())).thenReturn(List.of(
                new PlacePopularityInfo(101L, "카페", 5L, LocalDate.of(2026, 8, 1)),
                new PlacePopularityInfo(102L, "음식점", 3L, LocalDate.of(2026, 8, 1))
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(visitedPlaceLookupPort.findVisitedPlaceIds(1L)).thenReturn(Set.of());
        when(placePhotoService.findThumbnails(any())).thenReturn(Map.of());

        // when
        RecommendationInfo info = sut.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then — 전체 = 카페(101)·음식점(102) 카테고리 추천을 합친 것, 인기순 유지
        assertThat(info.allPlaces())
                .extracting(kr.chapchap.recommendation.application.info.RecommendedPlaceInfo::placeId)
                .containsExactly(101L, 102L);
    }

    @Test
    void 카테고리는_후보가_없어도_카탈로그_전체_목록을_빈_배열로_내려준다() {
        // given
        when(placeRadiusLookupPort.findWithinRadius(37.5665, 126.9780, 1000)).thenReturn(List.of(
                new NearbyPlaceInfo(101L, "투썸플레이스", "역삼동", null, 37.5447, 127.0557, "ChIJtest101"),
                new NearbyPlaceInfo(102L, "국밥집", "역삼동", null, 37.4999, 127.0364, null)
        ));
        when(popularityLookupPort.aggregateByPlaceIds(any())).thenReturn(List.of(
                new PlacePopularityInfo(101L, "카페", 5L, LocalDate.of(2026, 8, 1)),
                new PlacePopularityInfo(102L, "음식점", 3L, LocalDate.of(2026, 8, 1))
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(visitedPlaceLookupPort.findVisitedPlaceIds(1L)).thenReturn(Set.of());
        when(placePhotoService.findThumbnails(any())).thenReturn(Map.of());

        // when
        RecommendationInfo info = sut.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then — 후보가 없는 "운동" 카테고리도 빈 배열로 응답에 포함되어야 함
        assertThat(info.categoryPlaces())
                .extracting(RecommendationInfo.CategoryPlacesInfo::category)
                .containsExactly("카페", "음식점", "운동");

        List<Long> exercisePlaceIds = info.categoryPlaces().stream()
                .filter(category -> category.category().equals("운동"))
                .findFirst().orElseThrow()
                .places().stream()
                .map(kr.chapchap.recommendation.application.info.RecommendedPlaceInfo::placeId)
                .toList();
        assertThat(exercisePlaceIds).isEmpty();
    }
}
