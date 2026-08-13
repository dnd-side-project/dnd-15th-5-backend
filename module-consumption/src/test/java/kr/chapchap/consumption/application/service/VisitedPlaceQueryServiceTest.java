package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.PlaceLocationInfo;
import kr.chapchap.consumption.application.info.VisitedPlaceMarkerInfo;
import kr.chapchap.consumption.application.port.PlaceLocationLookupPort;
import kr.chapchap.consumption.application.port.PlaceNameLookupPort;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitedPlaceQueryServiceTest {

    @Mock
    private ConsumptionQueryRepository consumptionQueryRepository;

    @Mock
    private PlaceNameLookupPort placeNameLookupPort;

    @Mock
    private PlaceLocationLookupPort placeLocationLookupPort;

    private VisitedPlaceQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new VisitedPlaceQueryService(consumptionQueryRepository, placeNameLookupPort, placeLocationLookupPort);
    }

    @Test
    void 방문한_장소가_없으면_빈_리스트를_반환하고_이름_위치_조회는_하지_않는다() {
        // given
        when(consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null)).thenReturn(List.of());

        // when
        List<VisitedPlaceMarkerInfo> result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result).isEmpty();
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

        // when
        List<VisitedPlaceMarkerInfo> result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result)
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

        // when
        List<VisitedPlaceMarkerInfo> result = sut.getVisitedPlaceMarkers(1L, null);

        // then
        assertThat(result).extracting(VisitedPlaceMarkerInfo::placeName).containsExactly("알 수 없는 가게");
    }
}
