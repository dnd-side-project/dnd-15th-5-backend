package kr.chapchap.place.application.service;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.command.PlaceResolveCommand;
import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.application.port.AdministrativeDongLookupPort;
import kr.chapchap.place.application.port.PlaceCreatePort;
import kr.chapchap.place.domain.entity.Place;
import kr.chapchap.place.domain.repository.PlaceRepository;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceResolveServiceTest {

    private static final PlaceResolveCommand COMMAND = new PlaceResolveCommand(
            "google-place-id",
            "테스트 가게",
            "서울 강남구 테헤란로 1",
            37.5001,
            127.0365
    );

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private AdministrativeDongLookupPort administrativeDongLookupPort;

    @Mock
    private PlaceCreatePort placeCreatePort;

    private PlaceResolveService sut;

    @BeforeEach
    void setUp() {
        sut = new PlaceResolveService(
                placeRepository,
                administrativeDongLookupPort,
                placeCreatePort
        );
    }

    @Test
    void Google_Place_ID가_이미_있으면_SGIS를_호출하지_않고_기존_ID를_반환한다() {
        // given
        Place existingPlace = Place.create(
                COMMAND.googlePlaceId(),
                "기존 가게",
                "서울 강남구 기존로 1",
                "11680640",
                "역삼1동",
                37.49,
                127.03
        );
        ReflectionTestUtils.setField(existingPlace, "id", 101L);
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.of(existingPlace));

        // when
        Long result = sut.resolveOrCreate(COMMAND);

        // then
        assertThat(result).isEqualTo(101L);
        verifyNoInteractions(administrativeDongLookupPort, placeCreatePort);
    }

    @Test
    void 신규_장소면_도로명주소의_행정동과_프론트_위경도로_장소를_생성한다() {
        // given
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.empty());
        when(administrativeDongLookupPort.findByRoadAddress(COMMAND.roadAddress()))
                .thenReturn(new AdministrativeDongInfo("11680640", "역삼1동"));
        when(placeCreatePort.createOrGet(any(Place.class))).thenReturn(102L);

        // when
        Long result = sut.resolveOrCreate(COMMAND);

        // then
        assertThat(result).isEqualTo(102L);
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
        verify(placeCreatePort).createOrGet(captor.capture());
        Place createdPlace = captor.getValue();
        assertThat(createdPlace.getGooglePlaceId()).isEqualTo(COMMAND.googlePlaceId());
        assertThat(createdPlace.getName()).isEqualTo(COMMAND.name());
        assertThat(createdPlace.getRoadAddress()).isEqualTo(COMMAND.roadAddress());
        assertThat(createdPlace.getAdministrativeDongCode()).isEqualTo("11680640");
        assertThat(createdPlace.getAdministrativeDongName()).isEqualTo("역삼1동");
        assertThat(createdPlace.getLatitude()).isEqualTo(COMMAND.latitude());
        assertThat(createdPlace.getLongitude()).isEqualTo(COMMAND.longitude());
        assertThat(createdPlace.getLocation().getSRID()).isEqualTo(4326);
    }

    @Test
    void 도로명주소의_행정동을_찾지_못하면_좌표로_재조회한다() {
        // given
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.empty());
        when(administrativeDongLookupPort.findByRoadAddress(COMMAND.roadAddress()))
                .thenThrow(new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED));
        when(administrativeDongLookupPort.findByCoordinates(
                COMMAND.latitude(),
                COMMAND.longitude()
        )).thenReturn(new AdministrativeDongInfo("11590620", "사당1동"));
        when(placeCreatePort.createOrGet(any(Place.class))).thenReturn(103L);

        // when
        Long result = sut.resolveOrCreate(COMMAND);

        // then
        assertThat(result).isEqualTo(103L);
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
        verify(placeCreatePort).createOrGet(captor.capture());
        assertThat(captor.getValue().getAdministrativeDongCode()).isEqualTo("11590620");
        assertThat(captor.getValue().getAdministrativeDongName()).isEqualTo("사당1동");
    }

    @Test
    void 도로명주소_검색이_실패하면_좌표로_재조회한다() {
        // given
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.empty());
        when(administrativeDongLookupPort.findByRoadAddress(COMMAND.roadAddress()))
                .thenThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));
        when(administrativeDongLookupPort.findByCoordinates(
                COMMAND.latitude(),
                COMMAND.longitude()
        )).thenReturn(new AdministrativeDongInfo("11590620", "사당1동"));
        when(placeCreatePort.createOrGet(any(Place.class))).thenReturn(104L);

        // when
        Long result = sut.resolveOrCreate(COMMAND);

        // then
        assertThat(result).isEqualTo(104L);
        verify(administrativeDongLookupPort).findByCoordinates(
                COMMAND.latitude(),
                COMMAND.longitude()
        );
    }

    @Test
    void 좌표_조회도_실패하면_좌표_조회_예외를_그대로_반환한다() {
        // given
        BusinessException coordinateException =
                new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED);
        when(placeRepository.findByGooglePlaceId(COMMAND.googlePlaceId()))
                .thenReturn(Optional.empty());
        when(administrativeDongLookupPort.findByRoadAddress(COMMAND.roadAddress()))
                .thenThrow(new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));
        when(administrativeDongLookupPort.findByCoordinates(
                COMMAND.latitude(),
                COMMAND.longitude()
        )).thenThrow(coordinateException);

        // when & then
        assertThatThrownBy(() -> sut.resolveOrCreate(COMMAND))
                .isSameAs(coordinateException);
        verifyNoInteractions(placeCreatePort);
    }
}
