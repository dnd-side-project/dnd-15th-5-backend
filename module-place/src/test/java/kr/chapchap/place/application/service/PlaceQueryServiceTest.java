package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.PlaceLocationInfo;
import kr.chapchap.place.domain.entity.Place;
import kr.chapchap.place.domain.repository.PlaceRepository;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceQueryServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private PlaceRepository placeRepository;

    private PlaceQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceQueryService(placeRepository);
    }

    @Test
    void 요청한_모든_장소의_위치를_찾으면_placeId를_키로_하는_맵을_반환한다() {
        // given
        Place place101 = createPlace(101L, 127.0557, 37.5447);
        Place place102 = createPlace(102L, 129.0756, 35.1796);

        when(placeRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(place101, place102));

        // when
        Map<Long, PlaceLocationInfo> result = sut.findLocationsByIds(List.of(101L, 102L));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(101L).latitude()).isEqualTo(37.5447);
        assertThat(result.get(101L).longitude()).isEqualTo(127.0557);
    }

    @Test
    void 요청한_장소_중_하나라도_위치를_못_찾으면_예외를_던진다() {
        // given
        Place place101 = createPlace(101L, 127.0557, 37.5447);
        // 102L은 삭제됐거나 존재하지 않는다고 가정 — repository가 101L만 돌려줌
        when(placeRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(place101));

        // when & then
        assertThatThrownBy(() -> sut.findLocationsByIds(List.of(101L, 102L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.LOCATION_NOT_FOUND);
    }

    private Place createPlace(Long id, double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        Place place = Place.builder()
                .name("테스트 가게")
                .roadAddress("서울 성동구 테스트로 1")
                .administrativeDongCode("1120510100")
                .administrativeDongName("성수동")
                .location(point)
                .build();
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }
}
