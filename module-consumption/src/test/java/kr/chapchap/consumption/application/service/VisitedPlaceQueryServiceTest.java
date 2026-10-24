package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlacePopularityInfo;
import kr.chapchap.consumption.application.info.PlaceSummaryInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkersInfo;
import kr.chapchap.consumption.application.port.ConsumptionImageStorage;
import kr.chapchap.consumption.application.port.PlaceLikeLookupPort;
import kr.chapchap.consumption.application.port.PlaceSummaryLookupPort;
import kr.chapchap.consumption.domain.entity.ConsumptionImage;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.entity.PlaceLatestImageRow;
import kr.chapchap.consumption.domain.entity.PlaceLatestStickerRow;
import kr.chapchap.consumption.domain.entity.StickerItem;
import kr.chapchap.consumption.domain.repository.ConsumptionImageRepository;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitedPlaceQueryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-08-13T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private ConsumptionQueryRepository consumptionQueryRepository;

    @Mock
    private PlaceSummaryLookupPort placeSummaryLookupPort;

    @Mock
    private PlaceLikeLookupPort placeLikeLookupPort;

    @Mock
    private StickerQueryService stickerQueryService;

    @Mock
    private ConsumptionImageRepository consumptionImageRepository;

    @Mock
    private ConsumptionImageStorage consumptionImageStorage;

    @Mock
    private PopularityQueryService popularityQueryService;

    @Mock
    private StickerItem stickerItem;

    @Mock
    private ConsumptionImage consumptionImage;

    private VisitedPlaceQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new VisitedPlaceQueryService(
                consumptionQueryRepository, placeSummaryLookupPort,
                placeLikeLookupPort, stickerQueryService,
                consumptionImageRepository, consumptionImageStorage,
                popularityQueryService, fixedClock);
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
        verifyNoInteractions(placeSummaryLookupPort);
    }

    @Test
    void 사진_기록도_스티커도_없는_장소는_핀_비주얼이_모두_비어있다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 1L)
        ));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("테스트카페", "역삼동", "주소", 37.5, 127.0)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.findLatestImageIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.findLatestStickerItemsByPlace(eq(1L), any())).thenReturn(List.of());

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result.markers()).hasSize(1);
        VisitedPlaceMarkerInfo marker = result.markers().getFirst();
        assertThat(marker.placeId()).isEqualTo(101L);
        assertThat(marker.visitCount()).isEqualTo(1L);
        assertThat(marker.stickerName()).isNull();
        assertThat(marker.imageUrl()).isNull();
        verifyNoInteractions(stickerQueryService);
    }

    @Test
    void 사진_기록이_있으면_스티커보다_사진을_우선해서_핀에_노출한다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 2L)
        ));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("테스트카페", "역삼동", "주소", 37.5, 127.0)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.findLatestImageIdsByPlace(eq(1L), any()))
                .thenReturn(List.of(new PlaceLatestImageRow(101L, 7L)));
        when(consumptionImageRepository.findAllByIdInAndUserId(eq(List.of(7L)), eq(1L)))
                .thenReturn(List.of(consumptionImage));
        when(consumptionImage.getId()).thenReturn(7L);
        when(consumptionImage.getObjectKey()).thenReturn("consumption-images/7");
        when(consumptionImageStorage.createReadUrl("consumption-images/7")).thenReturn("https://cdn.example.com/7");

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        VisitedPlaceMarkerInfo marker = result.markers().getFirst();
        assertThat(marker.imageUrl()).isEqualTo("https://cdn.example.com/7");
        assertThat(marker.stickerName()).isNull();
        verifyNoInteractions(stickerQueryService);
    }

    @Test
    void 방문_횟수가_많은_순으로_마커를_정렬해서_반환한다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 2L),
                new PlaceCategoryVisitRow(102L, "음식점", 5L)
        ));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557),
                102L, new PlaceSummaryInfo("국밥집", "역삼동", "주소2", 37.4999, 127.0364)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());
        when(consumptionQueryRepository.findLatestImageIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.findLatestStickerItemsByPlace(eq(1L), any())).thenReturn(List.of(
                new PlaceLatestStickerRow(101L, 3L),
                new PlaceLatestStickerRow(102L, 3L)
        ));
        when(stickerQueryService.findItems(any())).thenReturn(Map.of(3L, stickerItem));
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
    void 장소_정보_조회에_실패한_장소가_있으면_위치정보없음_예외를_던진다() {
        // given: name/location이 한 번에 조회되므로, 장소 정보 자체가 없으면(=위치도 없으므로) 예외를 던진다
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 1L)
        ));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of()); // 조회 실패(빈 맵)
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());

        // when & then
        assertThatThrownBy(() -> sut.getVisitedPlaceMarkers(1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.PLACE_LOCATION_NOT_FOUND);
    }

    @Test
    void 위도만_있고_경도가_없는_장소가_있으면_위치정보없음_예외를_던진다() {
        // given: latitude만 검증하고 longitude를 빠뜨리면 안 됨
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 1L)
        ));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, null)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of());

        // when & then
        assertThatThrownBy(() -> sut.getVisitedPlaceMarkers(1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ConsumptionErrorCode.PLACE_LOCATION_NOT_FOUND);
    }

    @Test
    void 좋아요한_장소는_liked가_true고_최신_스티커_이름이_채워진다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 3L)
        ));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557, "ChIJtest101")
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of(101L));
        when(consumptionQueryRepository.findLatestImageIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.findLatestStickerItemsByPlace(eq(1L), any()))
                .thenReturn(List.of(new PlaceLatestStickerRow(101L, 3L)));
        when(stickerQueryService.findItems(any())).thenReturn(Map.of(3L, stickerItem));
        when(stickerItem.getName()).thenReturn("도넛");
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        VisitedPlaceMarkerInfo marker = result.markers().get(0);
        assertThat(marker.liked()).isTrue();
        assertThat(marker.stickerName()).isEqualTo("도넛");
        assertThat(marker.googlePlaceId()).isEqualTo("ChIJtest101");
    }

    @Test
    void 방문한_적_없고_다른_유저_소비기록도_없는_좋아요_장소는_카테고리_없이_마커로_포함된다() {
        // given: 101L은 방문 기록 있음, 102L은 방문 기록 없이 좋아요만 함(다른 유저 소비기록도 없음)
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 5L)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of(102L));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557),
                102L, new PlaceSummaryInfo("국밥집", "역삼동", "주소2", 37.4999, 127.0364)
        ));
        when(consumptionQueryRepository.findLatestImageIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.findLatestStickerItemsByPlace(eq(1L), any()))
                .thenReturn(List.of(new PlaceLatestStickerRow(101L, 3L)));
        when(stickerQueryService.findItems(any())).thenReturn(Map.of(3L, stickerItem));
        when(popularityQueryService.aggregatePopularityByPlaceIds(List.of(102L))).thenReturn(List.of());
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
    void 방문한_적_없어도_다른_유저가_소비기록을_남긴_좋아요_장소는_카테고리가_채워진다() {
        // given: 102L은 내가 방문한 적 없지만, 다른 유저가 거기서 소비한 기록이 있어 카테고리를 끌어올 수 있음
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of(
                new PlaceCategoryVisitRow(101L, "카페", 5L)
        ));
        when(placeLikeLookupPort.findLikedPlaceIds(1L)).thenReturn(Set.of(102L));
        when(placeSummaryLookupPort.findSummaries(any())).thenReturn(Map.of(
                101L, new PlaceSummaryInfo("투썸플레이스", "역삼동", "주소1", 37.5447, 127.0557),
                102L, new PlaceSummaryInfo("신논현 헬스장 매점", "신논현동", "주소2", 37.5045, 127.0248)
        ));
        when(consumptionQueryRepository.findLatestImageIdsByPlace(eq(1L), any())).thenReturn(List.of());
        when(consumptionQueryRepository.findLatestStickerItemsByPlace(eq(1L), any()))
                .thenReturn(List.of(new PlaceLatestStickerRow(101L, 3L)));
        when(stickerQueryService.findItems(any())).thenReturn(Map.of(3L, stickerItem));
        when(popularityQueryService.aggregatePopularityByPlaceIds(List.of(102L))).thenReturn(List.of(
                new PlacePopularityInfo(102L, "편의점/마트", 1L, LocalDate.of(2026, 7, 23))
        ));
        when(consumptionQueryRepository.countDistinctPlacesByUserAndDateRange(eq(1L), any(), any())).thenReturn(0L);

        // when
        VisitedPlaceMarkersInfo result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        VisitedPlaceMarkerInfo likedOnly = result.markers().stream()
                .filter(marker -> marker.placeId().equals(102L))
                .findFirst().orElseThrow();
        assertThat(likedOnly.category()).isEqualTo("편의점/마트");
        assertThat(likedOnly.visitCount()).isZero();
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
}
