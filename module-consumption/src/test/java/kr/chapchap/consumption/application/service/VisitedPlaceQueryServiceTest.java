package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlaceLocationInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.port.PlaceLikeLookupPort;
import kr.chapchap.consumption.application.port.PlaceLocationLookupPort;
import kr.chapchap.consumption.application.port.PlaceNameLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceFirstStickerRow;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.domain.repository.StickerItemRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitedPlaceQueryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-08-13T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private ConsumptionQueryRepository consumptionQueryRepository;

    @Mock
    private PlaceNameLookupPort placeNameLookupPort;

    @Mock
    private PlaceLocationLookupPort placeLocationLookupPort;

    @Mock
    private PlaceLikeLookupPort placeLikeLookupPort;

    @Mock
    private StickerItemRepository stickerItemRepository;

    private VisitedPlaceQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new VisitedPlaceQueryService(
                consumptionQueryRepository, placeNameLookupPort, placeLocationLookupPort,
                placeLikeLookupPort, stickerItemRepository, fixedClock);
    }

    @Test
    void 방문도_좋아요도_없으면_빈_마커_리스트를_반환하고_이름_위치_조회는_하지_않는다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of());
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(
                eq(1L), eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 9, 1))))
                .thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result.markers()).isEmpty();
        verifyNoInteractions(placeNameLookupPort, placeLocationLookupPort);
    }

    @Test
    void 방문_횟수가_많은_순으로_마커를_정렬해서_반환한다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 2L),
                new PlaceCategoryVisitRow(102L, "음식점", 5L)
        ));
        when(placeNameLookupPort.findNames(any())).thenReturn(Map.of(101L, "투썸플레이스", 102L, "국밥집"));
        when(placeLocationLookupPort.findLocations(any())).thenReturn(Map.of(
                101L, new PlaceLocationInfo(101L, 37.5447, 127.0557),
                102L, new PlaceLocationInfo(102L, 37.4999, 127.0364)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.findFirstStickerItemIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result.markers())
                .extracting(VisitedPlaceMarkerInfo::placeId, VisitedPlaceMarkerInfo::visitCount)
                .containsExactly(
                        tuple(102L, 5L),
                        tuple(101L, 2L)
                );
    }

    @Test
    void 이름_조회에_실패한_장소는_알_수_없는_가게로_대체된다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 1L)
        ));
        when(placeNameLookupPort.findNames(any())).thenReturn(Map.of()); // 이름 조회 실패(빈 맵)
        when(placeLocationLookupPort.findLocations(any())).thenReturn(Map.of(
                101L, new PlaceLocationInfo(101L, 37.5447, 127.0557)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.findFirstStickerItemIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result.markers()).extracting(VisitedPlaceMarkerInfo::placeName).containsExactly("알 수 없는 가게");
    }

    @Test
    void 좋아요한_장소는_liked가_true고_처음_받은_스티커_이름이_채워진다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 3L)
        ));
        when(placeNameLookupPort.findNames(any())).thenReturn(Map.of(101L, "투썸플레이스"));
        when(placeLocationLookupPort.findLocations(any())).thenReturn(Map.of(
                101L, new PlaceLocationInfo(101L, 37.5447, 127.0557)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of(101L));
        when(consumptionQueryRepository.findFirstStickerItemIdsByPlace(eq(1L), any()))
                .thenReturn(List.of(new PlaceFirstStickerRow(101L, 3L)));
        StickerItem donut = createStickerItem(3L, "도넛");
        when(stickerItemRepository.findAllById(any())).thenReturn(List.of(donut));
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        VisitedPlaceMarkerInfo marker = result.markers().get(0);
        assertThat(marker.liked()).isTrue();
        assertThat(marker.stickerName()).isEqualTo("도넛");
    }

    @Test
    void 방문한_적_없어도_좋아요한_장소는_카테고리_없이_마커로_포함된다() {
        // given: 101L은 방문 기록 있음, 102L은 방문 기록 없이 좋아요만 함
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 5L)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of(102L));
        when(placeNameLookupPort.findNames(any())).thenReturn(Map.of(101L, "투썸플레이스", 102L, "국밥집"));
        when(placeLocationLookupPort.findLocations(any())).thenReturn(Map.of(
                101L, new PlaceLocationInfo(101L, 37.5447, 127.0557),
                102L, new PlaceLocationInfo(102L, 37.4999, 127.0364)
        ));
        when(consumptionQueryRepository.findFirstStickerItemIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result.markers()).hasSize(2);
        VisitedPlaceMarkerInfo likedOnly = result.markers().stream()
                .filter(marker -> marker.placeId().equals(102L))
                .findFirst().orElseThrow();
        assertThat(likedOnly.liked()).isTrue();
        assertThat(likedOnly.category()).isNull();
        assertThat(likedOnly.visitCount()).isZero();
        assertThat(likedOnly.stickerName()).isNull();
    }

    @Test
    void 현재_월을_month_필드로_반환한다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of());
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then (fixedClock = 2026-08-13)
        assertThat(result.month()).isEqualTo(8);
    }

    @Test
    void 이번_달_방문한_서로_다른_장소_수를_monthlyPlaceCount로_반환한다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of());
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(
                eq(1L), eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 9, 1))))
                .thenReturn(3L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result.monthlyPlaceCount()).isEqualTo(3);
    }

    private StickerItem createStickerItem(Long id, String name) {
        StickerItem stickerItem = mock(StickerItem.class);
        when(stickerItem.getId()).thenReturn(id);
        when(stickerItem.getName()).thenReturn(name);
        return stickerItem;
    }
}
